package com.enterprise.ticketing.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    @NotBlank
    private String apiBasePath = "/api";

    private final Auth auth = new Auth();
    private final Ai ai = new Ai();
    private final Qdrant qdrant = new Qdrant();
    private final Knowledge knowledge = new Knowledge();
    private final Temporal temporal = new Temporal();
    private final Observability observability = new Observability();
    private final Modules modules = new Modules();

    public String getApiBasePath() {
        return apiBasePath;
    }

    public void setApiBasePath(String apiBasePath) {
        this.apiBasePath = apiBasePath;
    }

    public Qdrant getQdrant() {
        return qdrant;
    }

    public Auth getAuth() {
        return auth;
    }

    public Ai getAi() {
        return ai;
    }

    public Knowledge getKnowledge() {
        return knowledge;
    }

    public Temporal getTemporal() {
        return temporal;
    }

    public Observability getObservability() {
        return observability;
    }

    public Modules getModules() {
        return modules;
    }

    public static class Auth {
        private final Jwt jwt = new Jwt();

        public Jwt getJwt() {
            return jwt;
        }

        public static class Jwt {
            @NotBlank
            private String issuer = "enterprise-ai-ticketing";

            @NotBlank
            private String secret = "change-this-dev-secret-to-at-least-32-chars";

            @NotNull
            private Duration accessTokenTtl = Duration.ofHours(8);

            public String getIssuer() {
                return issuer;
            }

            public void setIssuer(String issuer) {
                this.issuer = issuer;
            }

            public String getSecret() {
                return secret;
            }

            public void setSecret(String secret) {
                this.secret = secret;
            }

            public Duration getAccessTokenTtl() {
                return accessTokenTtl;
            }

            public void setAccessTokenTtl(Duration accessTokenTtl) {
                this.accessTokenTtl = accessTokenTtl;
            }
        }
    }

    public static class Ai {
        private boolean enabled = true;
        private int retrievalTopK = 4;
        private final Provider provider = new Provider();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRetrievalTopK() {
            return retrievalTopK;
        }

        public void setRetrievalTopK(int retrievalTopK) {
            this.retrievalTopK = retrievalTopK;
        }

        public Provider getProvider() {
            return provider;
        }

        public static class Provider {
            @NotBlank
            private String type = "rule-based";

            @NotBlank
            private String model = "mvp-rule-based";

            private String baseUrl;
            private String apiKey;
            private String chatPath = "/v1/chat/completions";
            private final Local local = new Local();

            @NotNull
            private Duration timeout = Duration.ofSeconds(20);

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getModel() {
                return model;
            }

            public void setModel(String model) {
                this.model = model;
            }

            public String getBaseUrl() {
                return baseUrl;
            }

            public void setBaseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
            }

            public String getApiKey() {
                return apiKey;
            }

            public void setApiKey(String apiKey) {
                this.apiKey = apiKey;
            }

            public String getChatPath() {
                return chatPath;
            }

            public void setChatPath(String chatPath) {
                this.chatPath = chatPath;
            }

            public Duration getTimeout() {
                return timeout;
            }

            public void setTimeout(Duration timeout) {
                this.timeout = timeout;
            }

            public Local getLocal() {
                return local;
            }

            public static class Local {
                private boolean enabled = false;
                @NotBlank
                private String type = "openai-compatible";
                @NotBlank
                private String model = "local-small";
                private String baseUrl;
                private String apiKey;
                private String chatPath = "/v1/chat/completions";
                @NotNull
                private Duration timeout = Duration.ofSeconds(20);

                public boolean isEnabled() {
                    return enabled;
                }

                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }

                public String getType() {
                    return type;
                }

                public void setType(String type) {
                    this.type = type;
                }

                public String getModel() {
                    return model;
                }

                public void setModel(String model) {
                    this.model = model;
                }

                public String getBaseUrl() {
                    return baseUrl;
                }

                public void setBaseUrl(String baseUrl) {
                    this.baseUrl = baseUrl;
                }

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }

                public String getChatPath() {
                    return chatPath;
                }

                public void setChatPath(String chatPath) {
                    this.chatPath = chatPath;
                }

                public Duration getTimeout() {
                    return timeout;
                }

                public void setTimeout(Duration timeout) {
                    this.timeout = timeout;
                }
            }
        }
    }

    public static class Qdrant {
        private String host = "localhost";
        private int httpPort = 6333;
        private int grpcPort = 6334;
        private String apiKey;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getHttpPort() {
            return httpPort;
        }

        public void setHttpPort(int httpPort) {
            this.httpPort = httpPort;
        }

        public int getGrpcPort() {
            return grpcPort;
        }

        public void setGrpcPort(int grpcPort) {
            this.grpcPort = grpcPort;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class Knowledge {
        private String collectionName = "knowledge_chunks";
        private int embeddingDimension = 256;
        private int chunkSize = 800;
        private int chunkOverlap = 120;
        private int defaultTopK = 5;
        private int maxTopK = 10;
        private String globalDepartment = "GLOBAL";
        private final Embedding embedding = new Embedding();

        public String getCollectionName() {
            return collectionName;
        }

        public void setCollectionName(String collectionName) {
            this.collectionName = collectionName;
        }

        public int getEmbeddingDimension() {
            return embeddingDimension;
        }

        public void setEmbeddingDimension(int embeddingDimension) {
            this.embeddingDimension = embeddingDimension;
        }

        public int getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        public int getChunkOverlap() {
            return chunkOverlap;
        }

        public void setChunkOverlap(int chunkOverlap) {
            this.chunkOverlap = chunkOverlap;
        }

        public int getDefaultTopK() {
            return defaultTopK;
        }

        public void setDefaultTopK(int defaultTopK) {
            this.defaultTopK = defaultTopK;
        }

        public int getMaxTopK() {
            return maxTopK;
        }

        public void setMaxTopK(int maxTopK) {
            this.maxTopK = maxTopK;
        }

        public String getGlobalDepartment() {
            return globalDepartment;
        }

        public void setGlobalDepartment(String globalDepartment) {
            this.globalDepartment = globalDepartment;
        }

        public Embedding getEmbedding() {
            return embedding;
        }

        public static class Embedding {
            @Deprecated
            private String provider = "hashing";
            @Deprecated
            private String model = "hashing-v1";
            private final Routing routing = new Routing();
            private final Local local = new Local();
            private final Commercial commercial = new Commercial();

            public String getProvider() {
                return provider;
            }

            public void setProvider(String provider) {
                this.provider = provider;
            }

            public String getModel() {
                return model;
            }

            public void setModel(String model) {
                this.model = model;
            }

            public Routing getRouting() {
                return routing;
            }

            public Local getLocal() {
                return local;
            }

            public Commercial getCommercial() {
                return commercial;
            }

            public static class Routing {
                @NotBlank
                private String mode = "local";

                public String getMode() {
                    return mode;
                }

                public void setMode(String mode) {
                    this.mode = mode;
                }
            }

            public static class Local {
                private boolean enabled = true;
                @NotBlank
                private String providerType = "ollama";
                @NotBlank
                private String model = "nomic-embed-text:latest";
                @NotBlank
                private String baseUrl = "http://127.0.0.1:11434";
                @NotBlank
                private String embeddingPath = "/api/embeddings";
                @NotNull
                private Duration timeout = Duration.ofSeconds(20);
                private int dimension = 768;

                public boolean isEnabled() {
                    return enabled;
                }

                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }

                public String getProviderType() {
                    return providerType;
                }

                public void setProviderType(String providerType) {
                    this.providerType = providerType;
                }

                public String getModel() {
                    return model;
                }

                public void setModel(String model) {
                    this.model = model;
                }

                public String getBaseUrl() {
                    return baseUrl;
                }

                public void setBaseUrl(String baseUrl) {
                    this.baseUrl = baseUrl;
                }

                public String getEmbeddingPath() {
                    return embeddingPath;
                }

                public void setEmbeddingPath(String embeddingPath) {
                    this.embeddingPath = embeddingPath;
                }

                public Duration getTimeout() {
                    return timeout;
                }

                public void setTimeout(Duration timeout) {
                    this.timeout = timeout;
                }

                public int getDimension() {
                    return dimension;
                }

                public void setDimension(int dimension) {
                    this.dimension = dimension;
                }
            }

            public static class Commercial {
                private boolean enabled = false;
                @NotBlank
                private String providerType = "openai";
                @NotBlank
                private String model = "text-embedding-3-large";
                @NotBlank
                private String baseUrl = "https://api.openai.com";
                private String apiKey;
                @NotBlank
                private String embeddingPath = "/v1/embeddings";
                @NotNull
                private Duration timeout = Duration.ofSeconds(20);
                private int dimension = 3072;

                public boolean isEnabled() {
                    return enabled;
                }

                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }

                public String getProviderType() {
                    return providerType;
                }

                public void setProviderType(String providerType) {
                    this.providerType = providerType;
                }

                public String getModel() {
                    return model;
                }

                public void setModel(String model) {
                    this.model = model;
                }

                public String getBaseUrl() {
                    return baseUrl;
                }

                public void setBaseUrl(String baseUrl) {
                    this.baseUrl = baseUrl;
                }

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }

                public String getEmbeddingPath() {
                    return embeddingPath;
                }

                public void setEmbeddingPath(String embeddingPath) {
                    this.embeddingPath = embeddingPath;
                }

                public Duration getTimeout() {
                    return timeout;
                }

                public void setTimeout(Duration timeout) {
                    this.timeout = timeout;
                }

                public int getDimension() {
                    return dimension;
                }

                public void setDimension(int dimension) {
                    this.dimension = dimension;
                }
            }
        }
    }

    public static class Temporal {
        private String host = "localhost";
        private int port = 7233;
        private String namespace = "default";
        private String taskQueue = "ticketing-mvp-task-queue";

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getTaskQueue() {
            return taskQueue;
        }

        public void setTaskQueue(String taskQueue) {
            this.taskQueue = taskQueue;
        }
    }

    public static class Observability {
        private String serviceName = "enterprise-ai-ticketing";
        private boolean traceEnabled = true;
        private boolean metricsEnabled = true;
        private boolean logCorrelationEnabled = true;

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public boolean isTraceEnabled() {
            return traceEnabled;
        }

        public void setTraceEnabled(boolean traceEnabled) {
            this.traceEnabled = traceEnabled;
        }

        public boolean isMetricsEnabled() {
            return metricsEnabled;
        }

        public void setMetricsEnabled(boolean metricsEnabled) {
            this.metricsEnabled = metricsEnabled;
        }

        public boolean isLogCorrelationEnabled() {
            return logCorrelationEnabled;
        }

        public void setLogCorrelationEnabled(boolean logCorrelationEnabled) {
            this.logCorrelationEnabled = logCorrelationEnabled;
        }
    }

    public static class Modules {
        private boolean auth = true;
        private boolean ticket = true;
        private boolean knowledge = true;
        private boolean ai = true;
        private boolean workflow = true;
        private boolean approval = true;
        private boolean observability = true;

        public boolean isAuth() {
            return auth;
        }

        public void setAuth(boolean auth) {
            this.auth = auth;
        }

        public boolean isTicket() {
            return ticket;
        }

        public void setTicket(boolean ticket) {
            this.ticket = ticket;
        }

        public boolean isKnowledge() {
            return knowledge;
        }

        public void setKnowledge(boolean knowledge) {
            this.knowledge = knowledge;
        }

        public boolean isAi() {
            return ai;
        }

        public void setAi(boolean ai) {
            this.ai = ai;
        }

        public boolean isWorkflow() {
            return workflow;
        }

        public void setWorkflow(boolean workflow) {
            this.workflow = workflow;
        }

        public boolean isApproval() {
            return approval;
        }

        public void setApproval(boolean approval) {
            this.approval = approval;
        }

        public boolean isObservability() {
            return observability;
        }

        public void setObservability(boolean observability) {
            this.observability = observability;
        }
    }
}
