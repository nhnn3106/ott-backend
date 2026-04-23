const relationshipService = require("../services/relationshipService");

const EXCHANGE_NAME = "relationship.events";
const QUEUE_NAME = "chat_service_relationship_updates";
const ROUTING_KEY = "relationship.#";

const handleRelationshipEvent = async (channel, msg, io) => {
  if (!msg) return;

  try {
    const content = JSON.parse(msg.content.toString());
    console.log(" [x] RelationshipConsumer: Received event:", content);

    // Sync database
    await relationshipService.updateRelationshipFromEvent(content);

    // Emit Realtime via Socket.IO to users involved
    if (io && content.requesterId && content.receiverId) {
      const payload = {
        ...content,
        timestamp: content.timestamp || new Date().toISOString(),
        targetUserIds: [content.requesterId, content.receiverId]
      };
      
      // Emit to each user's room
      io.to(`user:${content.requesterId}`).emit("cap_nhat_quan_he", payload);
      io.to(`user:${content.receiverId}`).emit("cap_nhat_quan_he", payload);
      
      console.log(` [x] RelationshipConsumer: Emitted realtime update to users: ${content.requesterId}, ${content.receiverId}`);
    }

    channel.ack(msg);
  } catch (err) {
    console.error(" [!] RelationshipConsumer: Error processing:", err.message);
    channel.nack(msg, false, true);
  }
};

const initRelationshipConsumer = async (channel, io) => {
  try {
    await channel.assertExchange(EXCHANGE_NAME, "topic", { durable: true });
    const q = await channel.assertQueue(QUEUE_NAME, { durable: true });
    await channel.bindQueue(q.queue, EXCHANGE_NAME, ROUTING_KEY);

    console.log(` [*] RelationshipConsumer: Listening on queue: ${q.queue}`);

    channel.consume(q.queue, (msg) => handleRelationshipEvent(channel, msg, io), { noAck: false });
  } catch (error) {
    console.error(" [!] RelationshipConsumer: Failed to initialize:", error.message);
    throw error;
  }
};

module.exports = { initRelationshipConsumer };
