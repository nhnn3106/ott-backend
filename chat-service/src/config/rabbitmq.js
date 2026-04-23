const amqp = require("amqplib");

const RABBITMQ_URL = process.env.RABBITMQ_URL || "amqp://localhost:5672";

let connectionPromise = null;
let channelPromise = null;

async function getChannel() {
  if (!connectionPromise) {
    connectionPromise = amqp.connect(RABBITMQ_URL).catch((error) => {
      connectionPromise = null;
      throw error;
    });
  }

  if (!channelPromise) {
    channelPromise = connectionPromise
      .then((conn) => conn.createChannel())
      .catch((error) => {
        channelPromise = null;
        throw error;
      });
  }

  return channelPromise;
}

async function publishToQueue(queueName, payload) {
  const channel = await getChannel();
  await channel.assertQueue(queueName, { durable: true });

  channel.sendToQueue(queueName, Buffer.from(JSON.stringify(payload)), {
    persistent: true,
    contentType: "application/json",
  });
}

module.exports = {
  publishToQueue,
};
