const Message = require("../models/Message");
const ConversationService = require("./conversationService");
const { PutObjectCommand } = require("@aws-sdk/client-s3");
const { getSignedUrl } = require("@aws-sdk/s3-request-presigner");
const { s3Client, bucketName } = require("../config/s3");
const { publishMessageSentEvent } = require("./analyticsPublisher");

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

  try {
    await publishMessageSentEvent({
      messageId: savedMessage.msg_id,
      userId: senderId,
      messageType: type,
    });
  } catch (error) {
    // Do not block chat delivery when analytics pipeline is unavailable
    console.warn(
      "[analytics] publish message.sent failed:",
      error?.message || error,
    );
  }

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
