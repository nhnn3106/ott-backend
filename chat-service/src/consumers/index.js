const { connectRabbitMQ } = require("../config/rabbitmq");
const { initUserConsumer } = require("./userConsumer");

const initAllConsumers = async () => {
  try {
    const { channel } = await connectRabbitMQ();
    
    // Initialize all specific consumers here
    await initUserConsumer(channel);
    
    console.log(" [✓] All RabbitMQ consumers initialized successfully");
  } catch (error) {
    console.error(" [!] Failed to initialize RabbitMQ consumers. Retrying in 5s...", error.message);
    setTimeout(initAllConsumers, 5000);
  }
};

module.exports = { initAllConsumers };
