const relationshipService = require("../services/relationshipService");

const EXCHANGE_NAME = "relationship.events";
const ROUTING_KEY = "relationship.#";

const handleRelationshipEvent = async (channel, msg, io) => {
  if (!msg) return;

  try {
    const content = JSON.parse(msg.content.toString());
    console.log(" [x] RelationshipConsumer: Received event:", content);

    // Sync database (idempotent update)
    const relationship = await relationshipService.updateRelationshipFromEvent(content);

    // Emit Realtime via Socket.IO to users involved
    // We use fields from the saved relationship to ensure consistent naming (requester_id, receiver_id)
    if (io && relationship) {
      console.log(` [x] RelationshipConsumer: Emitting realtime update to users: ${relationship.requester_id}, ${relationship.receiver_id}`);
      
      // Emit to each user's room
      io.to(`user:${relationship.requester_id}`).emit("cap_nhat_quan_he", relationship);
      io.to(`user:${relationship.receiver_id}`).emit("cap_nhat_quan_he", relationship);
    }

    channel.ack(msg);
  } catch (err) {
    console.error(" [!] RelationshipConsumer: Error processing:", err.message);
    // Use nack with requeue=false to avoid infinite loops on syntax errors
    channel.nack(msg, false, false);
  }
};

const initRelationshipConsumer = async (channel, io) => {
  try {
    await channel.assertExchange(EXCHANGE_NAME, "topic", { durable: true });
    
    // Use an exclusive queue with no name to ensure every instance of chat-service 
    // receives the event (Fanout/Broadcast pattern for WebSockets)
    const q = await channel.assertQueue("", { exclusive: true });
    await channel.bindQueue(q.queue, EXCHANGE_NAME, ROUTING_KEY);

    console.log(` [*] RelationshipConsumer: Listening for broadcast on exclusive queue: ${q.queue}`);

    channel.consume(q.queue, (msg) => handleRelationshipEvent(channel, msg, io), { noAck: false });
  } catch (error) {
    console.error(" [!] RelationshipConsumer: Failed to initialize:", error.message);
    throw error;
  }
};

module.exports = { initRelationshipConsumer };
