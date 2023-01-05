# SAMPLE OVERVIEW

This Java API provides asynchronous request/response interaction with Ember services.

Requests are encoded and passed in binary format over TCP or Aeron (UDP or Shared Memory) transport to Ember Service. API client subscribes to Ember Message bus and filters responses. Responses are decoded from binary format and delivered to client event handler.

Ember provides two kind of API samples:

* Simple short time interactions with Ember (for example order request transmission or positions download). In case of errors users are advised to simply to retry full operation (including ember connection).
* Production pipeline sample show how to stay connected to ember 24/7 and automatically reconnect. This production ready error handling adds complexity and not recommended for simple use cases.

This package contains samples of the first type, see project <LINK> for the sample of the second kind.  

Additional information can be found in Trading Data Model document.


# SAMPLE INDEX

This project includes the following generic communication samples:

* DuplexSample - how to send trading requests and receive events.
* PublicationSample - how to implement one-way flow of trading requests from client to Ember.
* SubscriptionSample -  how to implement one-way flow of trading events from Ember to client.

Also we included more down the earth practical samples:

* OrderSubmitSample, OrderCancelSample, OrderReplaceSample - how to perform simple trading actions
* CustomRiskUpdateSample - how to update risk limits from CSV file
* KillSwitchSample - how to halt trading in Ember OMS (emergency kill switch)
* PositionRequestSample - how to fetch positions from Ember OMS
* JournalReaderSample - how to read ember journal

More samples can be added in the future, we do not always update this README - please check included Java sources for more. 


# BUILD

This project references Ember Java API libraries located in private maven repository.

Please make sure that you define environment variables `NEXUS_USER` and `NEXUS_PASS` to Deltix repository credentials provided to you.

The following command runs Gradle build:

```
./gradlew clean build 
```

# RUN

Make sure Ember is running. This sample assumes it is running locally on port 8989 (default port).

The following command line launches DuplexSample:

```
./gradlew run
```