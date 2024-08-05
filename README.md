# Paidy Proxy Service for One-Frame API

This is a test task for Paidy, developed by Damir Akhverdiev, featuring a proxy service for the One-Frame API.

Thank you for reviewing this project.

## Setup

Below are the instructions for setting up and starting this application. In addition to the basic steps described in the task, I've added a Redis cache using Docker. Follow these steps to run the project:

1. **Pull the One-Frame API:**
   ```bash
   docker pull paidyinc/one-frame:latest
   ```

2. **Run the One-Frame Service:**
   ```bash
   docker run -p 8080:8080 paidyinc/one-frame
   ```

3. **Run Redis in Docker:**
   ```bash
   docker run -d -p 6379:6379 --name redis-container -v redis-data:/data redis redis-server --requirepass "forex_redis_pass"
   ```

   The password is hardcoded, which I will mention later. Ensure all ports are available. Port in proxy config was moved from 8080 to 8081

## Assumptions, Simplifications, and Choices

Working on this test task was a journey filled with various emotions. It was interesting to work with new technologies and challenging to write code in something completely different from my experienced technologies.

### Use Cases of the Forex Proxy

The main objectives of the Forex Proxy were to:

1. The service returns an exchange rate when provided with 2 supported currencies
2. The rate should not be older than 5 minutes
3. The service should support at least 10,000 successful requests per day with 1 API token

### Implementation Process

Starting with the first point, I analyzed the current project by investigating the frameworks, structure, and domain. I also properly studied Scala. There are many things I want to discuss and talk about.

After that, I began implementing the basic functionality. There were various thoughts about this proxy. As described in the One-Frame API, it could take more than two pairs and return an array of rates. I considered making the request structure flexible (e.g., `/rate?pair={1}&pair={n}...`) to redirect it to One-Frame, but as the task only required implementation for two currencies, I decided to keep it simple.

For this, the `OneFrameClient` was created in the project to make calls to the API. It is located in the `http/client` folder. The implementation can vary from company to company, but I've tried to adhere to the project's code style as much as possible. I added various types of response handling. The One-Frame API does not return status codes if a request is forbidden or bad, so I've added validation for these responses. The client has its errors, which are also mapped to service errors. Eventually, the proxy should return more descriptive status codes and messages.

I decided to rename a few files to improve readability, and this renaming did not affect the code flow.

I've also added a unit test to demonstrate the need to cover logic before going live. However, I didn't cover everything due to time constraints, and even without a deadline, I didn't want to stretch it. It's important to note: **TESTING IS NECESSARY**.

### Addressing Use Cases 2 and 3

We know that the token can work only for 1,000 requests as a limitation. To avoid exhausting it and making many requests to a third-party client, I decided to add Redis to cache requested rates. This reduces the number of API calls, ensuring the token is not used more than necessary. Rates are cached for 5 minutes; after that, the system makes a call. We store rates with key like `"rate:USD:JPY"` in JSON format.

One consideration is the timestamp returned by One-Frame as "Now." If it could return a slightly different value, should I have added logic to validate the timestamp from the API? I decided not to overcomplicate things, but this should be noted. If the API could return another timestamp, it should be handled.

### Token Handling

Initially, I thought of One-Frame as an internal service. There were many thoughts about whether it was connected to the user. If it was, how should we cache values and validate the token for users, etc.? Eventually, I understood that One-Frame is external, so services make this request, and it is not related to the user specifically. As such, we don't need to check the token since this information is not sensitive and is more for internal use.

### Redis Setup

Redis could be configured with various features, such as clusters, database numbers, replicas, etc., but for this trivial task, it is used in its default state just to save rates. However, I've added many configuration parameters to show it can be set up this way.

Regarding the Redis password, normally, the Redis configuration or at least the password is not located in the config file itself. Most of the time, it is placed on the server manually, and the service reads from it. In our case, the password is in the config. We could encrypt it and implement decrypting logic (even base64 encryption, just to show), but I'm unsure if it is needed here or just worth mentioning. While it should be close to a production version, password handling can be done in many ways. To summarize:

1. It can be read from a server file.
2. It can be encrypted in the config, with specific encryption and salt values.

## Proxy Structure and Flow Diagram

The proxy structure is as follows:

<img src="/struct.png?raw=true" style="background-color:white;" alt="structure_img">

The request flow is described in this flow diagram:

<img src="/flow.png?raw=true" style="background-color:white;" alt="flow_img">

## What could be improved

Even though I worked hard on this project, it is, of course, not ideal. 
In my opinion, the structure could be improved, and the classes and methods could be cleaner. Encoding and decoding were challenging for me, though they are not that hard.
More unit tests could be written, and access to Redis could be more secure. Hard-coded error messages, API prefixes also could be handled differently.
I would really like to see how a professional would implement this service or at least have a discussion about potential improvements.

Anyway, it was a fun experience.
