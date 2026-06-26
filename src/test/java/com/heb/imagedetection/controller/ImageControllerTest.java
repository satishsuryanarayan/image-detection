package com.heb.imagedetection.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heb.imagedetection.dto.CreateImageRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.main.banner-mode=off"
})
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateImageAndGenerateLabel() throws Exception {
        CreateImageRequest request = new CreateImageRequest();
        request.setImageUrl("https://example.com/cat-photo.jpg");
        request.setEnableDetection(true);

        mockMvc.perform(post("/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("cat photo"))
                .andExpect(jsonPath("$.detectedObjects[0]").value("cat"));
    }

    @Test
    void shouldReturnAllImagesWhenObjectsQueryIsMissing() throws Exception {
        CreateImageRequest first = new CreateImageRequest();
        first.setImageUrl("https://example.com/dog-park.png");
        first.setEnableDetection(true);

        CreateImageRequest second = new CreateImageRequest();
        second.setImageUrl("https://example.com/car-road.png");
        second.setEnableDetection(true);

        mockMvc.perform(post("/images")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(first))).andExpect(status().isOk());

        mockMvc.perform(post("/images")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(second))).andExpect(status().isOk());

        mockMvc.perform(get("/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[?(@.imageUrl=='https://example.com/dog-park.png')]").exists())
                .andExpect(jsonPath("$[?(@.imageUrl=='https://example.com/car-road.png')]").exists());

        mockMvc.perform(get("/images").param("objects", "dog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].detectedObjects[0]").value("dog"));
    }

    @Test
    void shouldReturnFullDetectedObjectsForMatchedImageSearch() throws Exception {
        CreateImageRequest request = new CreateImageRequest();
        request.setImageUrl("https://example.com/dog-tree-person.png");
        request.setEnableDetection(true);

        mockMvc.perform(post("/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/images").param("objects", "dog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].detectedObjects[0]").value("dog"))
                .andExpect(jsonPath("$[0].detectedObjects[1]").value("tree"))
                .andExpect(jsonPath("$[0].detectedObjects[2]").value("person"));
    }

    @Test
    void shouldReturnBadRequestForInvalidUrl() throws Exception {
        CreateImageRequest request = new CreateImageRequest();
        request.setImageUrl("not-a-url");

        mockMvc.perform(post("/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").exists());
    }

    @Test
    void shouldReturnBadRequestForMalformedJsonPayload() throws Exception {
        mockMvc.perform(post("/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"imageUrl\": https://example.com/image.jpg }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]").value("Malformed JSON request body"));
    }

    @Test
    void shouldReturnBadRequestForUnsupportedUrlProtocol() throws Exception {
        CreateImageRequest request = new CreateImageRequest();
        // The value is URL-shaped, but the API intentionally accepts only HTTP(S) image URLs.
        request.setImageUrl("ftp://example.com/cat-photo.jpg");

        mockMvc.perform(post("/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").exists());
    }

    @Test
    void shouldReturnNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/images/9999"))
                .andExpect(status().isNotFound());
    }
}