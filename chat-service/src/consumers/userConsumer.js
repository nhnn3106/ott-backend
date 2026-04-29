const User = require("../models/User");
const userService = require("../services/userService");

const EXCHANGE_NAME = "user.events";
const QUEUE_NAME = "chat_service_user_created";
const ROUTING_KEY = "user.created";

const handleUserEvent = async (channel, msg) => {
  if (!msg) return;

  try {
    const routingKey = msg.fields.routingKey;
    const content = JSON.parse(msg.content.toString());
    console.log(` [x] UserConsumer: Received ${routingKey} event:`, content);

    const { userId } = content;

    if (!userId) {
      console.warn(" [!] UserConsumer: Skipping event due to missing userId");
      return channel.ack(msg);
    }

    if (routingKey === "user.created") {
      await userService.createUser(content);
    } else if (routingKey === "user.updated") {
      await userService.updateUser(content);
    }

    console.log(` [v] UserConsumer: Processed ${routingKey} for ${userId}`);
    channel.ack(msg);
  } catch (err) {
    console.error(" [!] UserConsumer: Error processing message:", err.message);
    channel.nack(msg, false, true);
  }
};

const initUserConsumer = async (channel) => {
  try {
    await channel.assertExchange(EXCHANGE_NAME, "topic", { durable: true });
    const q = await channel.assertQueue(QUEUE_NAME, { durable: true });

    // Bind for both created and updated
    await channel.bindQueue(q.queue, EXCHANGE_NAME, "user.created");
    await channel.bindQueue(q.queue, EXCHANGE_NAME, "user.updated");

    console.log(` [*] UserConsumer: Listening for user events on queue: ${q.queue}`);

    channel.consume(q.queue, (msg) => handleUserEvent(channel, msg), { noAck: false });
  } catch (error) {
    console.error(" [!] UserConsumer: Failed to initialize:", error.message);
    throw error;
  }
};

module.exports = { initUserConsumer };
