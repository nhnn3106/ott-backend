const amqp = require("amqplib");

const RABBITMQ_HOST = process.env.RABBITMQ_HOST || "localhost";
const RABBITMQ_PORT = process.env.RABBITMQ_PORT || "5672";
const RABBITMQ_USER = process.env.RABBITMQ_USERNAME || "admin";
const RABBITMQ_PASS = process.env.RABBITMQ_PASSWORD || "rabbit123";

const RABBITMQ_URL = process.env.RABBITMQ_URL || `amqp://${RABBITMQ_USER}:${RABBITMQ_PASS}@${RABBITMQ_HOST}:${RABBITMQ_PORT}`;

let connection = null;
let channel = null;

const connectRabbitMQ = async () => {
  try {
    if (connection) return { connection, channel };

    connection = await amqp.connect(RABBITMQ_URL);
    channel = await connection.createChannel();

    console.log(" [✓] RabbitMQ: Connected successfully");

    connection.on("error", (err) => {
      console.error("[AMQP] Connection error:", err.message);
    });

    connection.on("close", () => {
      console.warn("[AMQP] Connection closed. Restarting process might be needed or handled by consumers.");
      connection = null;
      channel = null;
    });

    return { connection, channel };
  } catch (error) {
    console.error("[AMQP] Failed to connect to RabbitMQ:", error.message);
    throw error;
  }
};

const getChannel = () => channel;
const getConnection = () => connection;

module.exports = { connectRabbitMQ, getChannel, getConnection };
