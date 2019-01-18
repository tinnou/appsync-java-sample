package tinnou

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.appsync.model.AuthenticationType

data class AppSyncRequest @JvmOverloads constructor(val graphqlRequest: GraphQLRequest,
                                               val endpointUrl: String? = null,
                                               val headers: Map<String, String> = emptyMap(),
                                               val authType: AuthenticationType = AuthenticationType.API_KEY,
                                               val credentialsProvider: AWSCredentialsProvider? = null)

data class GraphQLRequest @JvmOverloads constructor(val query: String,
                                               val variables: Map<String, Any> = emptyMap(),
                                               val operationName: String? = null)


data class GraphQLResult constructor(val data: Map<String, Any>?,
                                val errors: List<GraphQLError>?,
                                val extensions: Map<String, Any>?)

data class GraphQLError @JvmOverloads constructor(var message: String? = null,
                                             var locations: List<SourceLocation>? = null,
                                             var path: List<Any>? = null,
                                             var errorType: String? = null,
                                             var errorInfo: Map<String, Any>? = null,
                                             var data: Any? = null)

data class SourceLocation @JvmOverloads constructor(val line: Int = 0,
                                               val column: Int = 0)
