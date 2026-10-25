package com.lms.course;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lms.course.dto.response.CourseDetailResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import static org.junit.jupiter.api.Assertions.*;

public class RedisSerializerTest {

    @Test
    void testDefaultGenericJackson2JsonRedisSerializer() {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        CourseDetailResponse original = CourseDetailResponse.builder()
                .id("test-id-123")
                .title("Test Course")
                .build();

        byte[] bytes = serializer.serialize(original);
        System.out.println("Serialized with no-arg: " + new String(bytes));

        Object deserialized = serializer.deserialize(bytes);
        assertNotNull(deserialized);
        assertTrue(deserialized instanceof CourseDetailResponse, "Expected CourseDetailResponse but got: " + deserialized.getClass());
        assertEquals("test-id-123", ((CourseDetailResponse) deserialized).getId());
    }

    @Test
    void testConfiguredObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);
        CourseDetailResponse original = CourseDetailResponse.builder()
                .id("test-id-456")
                .title("Test Course 2")
                .build();

        byte[] bytes = serializer.serialize(original);
        System.out.println("Serialized with configured mapper: " + new String(bytes));

        Object deserialized = serializer.deserialize(bytes);
        assertNotNull(deserialized);
        assertTrue(deserialized instanceof CourseDetailResponse, "Expected CourseDetailResponse but got: " + deserialized.getClass());
    }

    @Test
    void testListCategoryResponse() {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        com.lms.course.dto.response.CategoryResponse item = com.lms.course.dto.response.CategoryResponse.builder()
                .id("c_java").name("Java").build();
        java.util.List<com.lms.course.dto.response.CategoryResponse> list = new java.util.ArrayList<>();
        list.add(item);
        byte[] bytes = serializer.serialize(list);
        System.out.println("ArrayList serialized: " + new String(bytes));
        Object deserialized = serializer.deserialize(bytes);
        System.out.println("ArrayList deserialized: " + deserialized);

        // Test Collectors.toList() (java.util.ArrayList)
        java.util.List<com.lms.course.dto.response.CategoryResponse> streamList = java.util.stream.Stream.of(item)
                .collect(java.util.stream.Collectors.toList());
        byte[] streamBytes = serializer.serialize(streamList);
        System.out.println("Collectors.toList serialized: " + new String(streamBytes));
        Object streamDeserialized = serializer.deserialize(streamBytes);
        System.out.println("Collectors.toList deserialized: " + streamDeserialized);
        assertNotNull(streamDeserialized);
        assertTrue(streamDeserialized instanceof java.util.List);
    }
}
