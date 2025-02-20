package com.patrol.domain.LostFound.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QFindPost is a Querydsl query type for FindPost
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFindPost extends EntityPathBase<FindPost> {

    private static final long serialVersionUID = 656507839L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QFindPost findPost = new QFindPost("findPost");

    public final DatePath<java.time.LocalDate> birthDate = createDate("birthDate", java.time.LocalDate.class);

    public final StringPath breed = createString("breed");

    public final StringPath characteristics = createString("characteristics");

    public final StringPath content = createString("content");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath findTime = createString("findTime");

    public final NumberPath<Long> foundId = createNumber("foundId", Long.class);

    public final EnumPath<FindPost.Gender> gender = createEnum("gender", FindPost.Gender.class);

    public final NumberPath<Double> latitude = createNumber("latitude", Double.class);

    public final StringPath location = createString("location");

    public final NumberPath<Double> longitude = createNumber("longitude", Double.class);

    public final com.patrol.domain.LostPost.entity.QLostPost lostPost;

    public final NumberPath<Long> memberId = createNumber("memberId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> modifiedAt = createDateTime("modifiedAt", java.time.LocalDateTime.class);

    public final StringPath name = createString("name");

    public final NumberPath<Long> petId = createNumber("petId", Long.class);

    public final EnumPath<FindPost.Size> size = createEnum("size", FindPost.Size.class);

    public final EnumPath<FindPost.Status> status = createEnum("status", FindPost.Status.class);

    public final StringPath tags = createString("tags");

    public final StringPath title = createString("title");

    public QFindPost(String variable) {
        this(FindPost.class, forVariable(variable), INITS);
    }

    public QFindPost(Path<? extends FindPost> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QFindPost(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QFindPost(PathMetadata metadata, PathInits inits) {
        this(FindPost.class, metadata, inits);
    }

    public QFindPost(Class<? extends FindPost> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.lostPost = inits.isInitialized("lostPost") ? new com.patrol.domain.LostPost.entity.QLostPost(forProperty("lostPost")) : null;
    }

}

