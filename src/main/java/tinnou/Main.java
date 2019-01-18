package tinnou;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.google.common.collect.ImmutableMap;

import static com.google.common.collect.Lists.newArrayList;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    /**
     * AWS AppSync API credentials
     */
    private static final String apiKey = "xxxxxxxxxxxxxxx";
    private static final String region = "xxxxx";
    private static final String api = "https://xxxx.appsync-api.xxxx.amazonaws.com/graphql";

    /*
     * Mutation.putPost resolver:
     *  - datasource: none
     *  - request mapping template:
     *      {
     *          "version": "2017-02-28",
     *          "payload": {
     *              "id": "$ctx.args.id",
     *              "title": "$ctx.args.title"
     *          }
     *      }
     *  - response mapping template:
     *     $util.toJson($context.result)
     *
     */
    public static void main(String[] args) {
        ApiKeyExample apiKeyExample = new ApiKeyExample(apiKey, region, api);

        String mutation = " mutation PutPost($id:ID !, $title: String!) {putPost(id: $id, title: $title) {id title } }";
        Map<String, String> variables = ImmutableMap.of("id", "123", "title", "Hello World!");
        String operationName = "PutPost";

        GraphQLRequest graphQLRequest = new GraphQLRequest(mutation, variables, operationName);
        AppSyncRequest appSyncRequest = new AppSyncRequest(graphQLRequest);

        GraphQLResult graphQLResult = apiKeyExample.executePost(appSyncRequest);

        // should print:
        // [main] INFO tinnou.Main - result GraphQLResult(data={putPost={id=123, title=Hello World!}}, errors=null, extensions=null)
        log.info("result {}", graphQLResult);
    }

    public static class ApiKeyExample {

        private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new KotlinModule());
        private static final String API_KEY_HEADER = "x-api-key";
        private final HttpClient client;
        /**
         * AWS AppSync API credentials
         */
        private final String apiKey;
        private final String region;
        private final String api;

        public ApiKeyExample(final String apiKey, final String region, final String api) {
            this.apiKey = apiKey;
            this.region = region;
            this.api = api;
            this.client = HttpClientBuilder
                .create()
                .setDefaultHeaders(newArrayList(new BasicHeader(API_KEY_HEADER, this.apiKey)))
                .build();
        }

        private HttpPost getHttpPost(final String endpointUrl, final Map<String, String> headers) {
            final URI uri;
            try {
                uri = new URI(endpointUrl);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }

            final HttpPost request = new HttpPost(uri);

            if(null != headers && !headers.isEmpty()){
                for(Map.Entry<String, String> e : headers.entrySet()){
                    request.addHeader(e.getKey(), e.getValue());
                }
            }

            request.setConfig(RequestConfig.custom().setConnectionRequestTimeout(30).build());

            return request;
        }

        private HttpEntity prepareHttpEntity(final GraphQLRequest graphQLRequest) {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", graphQLRequest.getQuery());
            requestBody.put("variables", graphQLRequest.getVariables());

            if (graphQLRequest.getOperationName() != null) {
                requestBody.put("operationName", graphQLRequest.getOperationName());
            }

            try {
                return new StringEntity(MAPPER.writeValueAsString(requestBody), ContentType.APPLICATION_JSON);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        private GraphQLResult readResult(final InputStream inputStream) {
            try {
                return MAPPER.readValue(inputStream, GraphQLResult.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public GraphQLResult executePost(final AppSyncRequest appSyncRequest) {
            final HttpPost httpPost = getHttpPost(api, null);
            final HttpEntity httpEntity = prepareHttpEntity(appSyncRequest.getGraphqlRequest());
            httpPost.setEntity(httpEntity);

            HttpResponse httpResponse;
            try {
                httpResponse = client.execute(httpPost);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            InputStream content;
            try {
                content = httpResponse.getEntity().getContent();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return readResult(content);
        }
    }
}
