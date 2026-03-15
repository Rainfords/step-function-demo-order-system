package com.example.order.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sfn.SfnClient
import java.net.URI
import java.time.Duration

@Configuration
@Profile("local")
class AwsConfigLocal(
    @Value("\${aws.endpoint:http://localhost:4566}")
    private val endpoint: String,
    @Value("\${aws.region:us-east-1}")
    private val region: String
) {

    @Bean
    fun sfnClient(): SfnClient {
        val credentials = AwsBasicCredentials.create("test", "test")
        val httpClient = ApacheHttpClient.builder()
            .maxConnections(300)
            .connectionTimeout(Duration.ofSeconds(10))
            .socketTimeout(Duration.ofSeconds(90))
            .connectionMaxIdleTime(Duration.ofSeconds(30))
            .build()
        return SfnClient.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .httpClient(httpClient)
            .build()
    }
}

@Configuration
@Profile("!local")
class AwsConfigProd {

    @Bean
    fun sfnClient(): SfnClient {
        return SfnClient.builder()
            .region(Region.US_EAST_1)
            .build()
    }
}

@Configuration
class AwsProperties {
    @Value("\${aws.stepfunctions.state-machine-arn:arn:aws:states:us-east-1:000000000000:stateMachine:OrderProcessingWorkflow}")
    lateinit var stateMachineArn: String

    @Value("\${aws.stepfunctions.activities.validate:arn:aws:states:us-east-1:000000000000:activity:ValidateOrderActivity}")
    lateinit var validateActivityArn: String

    @Value("\${aws.stepfunctions.activities.inventory:arn:aws:states:us-east-1:000000000000:activity:ReserveInventoryActivity}")
    lateinit var inventoryActivityArn: String

    @Value("\${aws.stepfunctions.activities.payment:arn:aws:states:us-east-1:000000000000:activity:ProcessPaymentActivity}")
    lateinit var paymentActivityArn: String

    @Value("\${aws.stepfunctions.activities.fulfillment:arn:aws:states:us-east-1:000000000000:activity:FulfillOrderActivity}")
    lateinit var fulfillmentActivityArn: String

    @Value("\${aws.stepfunctions.activities.release:arn:aws:states:us-east-1:000000000000:activity:ReleaseInventoryActivity}")
    lateinit var releaseActivityArn: String
}
