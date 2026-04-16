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
    private final Qdrant qdrant = new Qdrant();
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
