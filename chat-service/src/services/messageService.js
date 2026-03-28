const Message = require("../models/Message");
const ConversationService = require("./conversationService");
const { PutObjectCommand } = require("@aws-sdk/client-s3");
const { getSignedUrl } = require("@aws-sdk/s3-request-presigner");
const { s3Client, bucketName } = require("../config/s3");

const crypto = require("crypto");

const buildReplyPreview = (message) => {
  if (!message) return null;

  const rawContent = Array.isArray(message.content)
    ? message.content[0] || ""
    : message.content || "";

  let preview = "";
  switch (message.type) {
    case "image":
      preview = "[Hình ảnh]";
      break;
    case "video":
      preview = "[Video]";
      break;
    case "file":
      preview = "[Tệp tin]";
      break;
    case "audio":
      preview = "[Âm thanh]";
      break;
    default:
      preview = rawContent;
      break;
  }

  return {
    msg_id: message.msg_id,
    sender_id: message.sender_id,
    type: message.type,
    content: preview.length > 120 ? preview.substring(0, 120) + "..." : preview,
    is_deleted: !!message.is_deleted,
    is_revoked: !!message.is_revoked,
  };
};

exports.generatePresignedUrl = async (fileName, fileType) => {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");

  const fileCategory = fileType.startsWith("image/")
    ? "image"
    : fileType.startsWith("video/")
      ? "video"
      : fileType.startsWith("audio/")
        ? "audio"
        : "file";

  const uniqueId = crypto.randomBytes(8).toString("hex");

  const key = `messages/${fileCategory}/${year}/${month}/${day}/${uniqueId}_${fileName}`;

  const command = new PutObjectCommand({
    Bucket: bucketName,
    Key: key,
    ContentType: fileType,
  });

  const uploadUrl = await getSignedUrl(s3Client, command, { expiresIn: 300 });

  return { uploadUrl, fileCategory, key };
};

exports.sendMessage = async ({
  conversationId,
  senderId,
  content,
  type,
  size,
  replyToMsgId,
}) => {
  // Nếu content đã là array (image keys) thì dùng trực tiếp, không thì wrap
  const contentArray = Array.isArray(content) ? content : [content];

  let replyMessage = null;
  if (replyToMsgId) {
    replyMessage = await Message.findOne({
      msg_id: replyToMsgId,
      conversation_id: conversationId,
    }).lean();

    if (!replyMessage) {
      throw new Error("Tin nhắn trả lời không hợp lệ");
    }
  }

  const newMessage = new Message({
    conversation_id: conversationId,
    sender_id: senderId,
    content: contentArray,
    type: type,
    size: size,
    reply_to_msg_id: replyToMsgId || null,
  });

  const savedMessage = await newMessage.save();
  const updatedConversation = await ConversationService.updateLastMessage(
    conversationId,
    savedMessage,
  );

  // Gửi kèm sender_name để FE cập nhật conversation list mà không cần query thêm
  return {
    ...savedMessage.toObject(),
    sender_name: updatedConversation?.last_message?.sender_name || "",
    reply_to: buildReplyPreview(replyMessage),
  };
};

exports.getMessageHistory = async (conversationId, deletedMsgId = "0") => {
  const messages = await Message.find({ conversation_id: conversationId }).sort(
    {
      msg_id: 1,
    },
  );

  const filteredMessages =
    !deletedMsgId || deletedMsgId === "0"
      ? messages
      : messages.filter((m) => BigInt(m.msg_id) > BigInt(deletedMsgId));

  const replyIds = [
    ...new Set(
      filteredMessages
        .map((m) => m.reply_to_msg_id)
        .filter((replyId) => typeof replyId === "string" && replyId.length > 0),
    ),
  ];

  if (replyIds.length === 0) {
    return filteredMessages.map((m) => ({ ...m.toObject(), reply_to: null }));
  }

  const referencedMessages = await Message.find({
    conversation_id: conversationId,
    msg_id: { $in: replyIds },
  }).lean();

  const referencedMap = new Map(
    referencedMessages.map((m) => [m.msg_id, buildReplyPreview(m)]),
  );

  return filteredMessages.map((m) => ({
    ...m.toObject(),
    reply_to: referencedMap.get(m.reply_to_msg_id) || null,
  }));
};

exports.reactToMessage = async ({
  conversationId,
  msgId,
  userId,
  reactionType,
}) => {
  const message = await Message.findOne({
    msg_id: msgId,
    conversation_id: conversationId,
  });

  if (!message) {
    throw new Error("Tin nhắn không tồn tại");
  }

  const normalizedReaction = String(reactionType || "").trim();
  if (!normalizedReaction) {
    throw new Error("Reaction không hợp lệ");
  }

  const existingReactionIndex = message.reactions.findIndex(
    (reaction) =>
      reaction.user_id === userId && reaction.type === normalizedReaction,
  );

  if (existingReactionIndex >= 0) {
    // Bấm lại cùng emoji thì bỏ reaction đó.
    message.reactions.splice(existingReactionIndex, 1);
  } else {
    // Cho phép 1 user có nhiều emoji reaction trên cùng 1 tin nhắn.
    message.reactions.push({
      user_id: userId,
      type: normalizedReaction,
    });
  }

  const updatedMessage = await message.save();

  return {
    _id: updatedMessage._id,
    msg_id: updatedMessage.msg_id,
    conversation_id: updatedMessage.conversation_id,
    reactions: updatedMessage.reactions,
  };
};

// Pin/Unpin message
exports.pinMessage = async ({ conversationId, msgId, userId, isPinned }) => {
  const message = await Message.findOne({
    msg_id: msgId,
    conversation_id: conversationId,
  });

  if (!message) {
    throw new Error("Tin nhắn không tồn tại");
  }

  message.is_pinned = isPinned;
  message.pinned_at = isPinned ? new Date() : null;
  message.pinned_by = isPinned ? userId : null;

  const updatedMessage = await message.save();

  return {
    _id: updatedMessage._id,
    msg_id: updatedMessage.msg_id,
    conversation_id: updatedMessage.conversation_id,
    is_pinned: updatedMessage.is_pinned,
    pinned_at: updatedMessage.pinned_at,
    pinned_by: updatedMessage.pinned_by,
    type: updatedMessage.type,
    content: updatedMessage.content,
    sender_id: updatedMessage.sender_id,
    createdAt: updatedMessage.createdAt,
  };
};

// Get pinned messages for a conversation
exports.getPinnedMessages = async (conversationId) => {
  const messages = await Message.find({
    conversation_id: conversationId,
    is_pinned: true,
  }).sort({ pinned_at: -1 });

  return messages;
};

// Get media messages (images/videos) for a conversation
exports.getMediaMessages = async (conversationId, limit = 20, skip = 0) => {
  const messages = await Message.find({
    conversation_id: conversationId,
    type: { $in: ["image", "video"] },
    is_deleted: false,
    is_revoked: false,
  })
    .sort({ createdAt: -1 })
    .skip(skip)
    .limit(limit);

  return messages;
};

// Get file messages for a conversation
exports.getFileMessages = async (conversationId, limit = 20, skip = 0) => {
  const messages = await Message.find({
    conversation_id: conversationId,
    type: "file",
    is_deleted: false,
    is_revoked: false,
  })
    .sort({ createdAt: -1 })
    .skip(skip)
    .limit(limit);

  return messages;
};

// Extract and get links from messages
exports.getLinkMessages = async (conversationId, limit = 20, skip = 0) => {
  // URL regex pattern
  const urlPattern = /https?:\/\/[^\s<>"{}|\\^`[\]]+/gi;

  const messages = await Message.find({
    conversation_id: conversationId,
    type: "text",
    is_deleted: false,
    is_revoked: false,
  }).sort({ createdAt: -1 });

  // Filter messages that contain links
  const messagesWithLinks = messages.filter((msg) => {
    const content = Array.isArray(msg.content) ? msg.content.join(" ") : msg.content;
    return urlPattern.test(content);
  });

  // Extract links from messages
  const linksData = messagesWithLinks.slice(skip, skip + limit).map((msg) => {
    const content = Array.isArray(msg.content) ? msg.content.join(" ") : msg.content;
    const links = content.match(urlPattern) || [];
    
    return {
      _id: msg._id,
      msg_id: msg.msg_id,
      links: links,
      sender_id: msg.sender_id,
      createdAt: msg.createdAt,
    };
  });

  return linksData;
};
