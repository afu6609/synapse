package com.synapse.embedding;

import com.synapse.embedding.model.*;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RegisterReflectionForBinding({
        Message.class,
        EmbeddingRequest.class,
        EmbeddingResult.class,
        SearchRequest.class,
        SearchResult.class,
        GraphAssociation.class
})
public class SynapseApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynapseApplication.class, args);
    }

}
