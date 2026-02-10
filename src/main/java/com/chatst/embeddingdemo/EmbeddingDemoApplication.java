package com.chatst.embeddingdemo;

import com.chatst.embeddingdemo.model.*;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RegisterReflectionForBinding({
        Message.class,
        EmbeddingRequest.class,
        EmbeddingResult.class,
        SearchRequest.class,
        SearchResult.class
})
public class EmbeddingDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmbeddingDemoApplication.class, args);
    }

}
