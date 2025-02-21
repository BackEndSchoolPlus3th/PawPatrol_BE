package com.patrol.domain.lostpost.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLostPost is a Querydsl query type for LostPost
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLostPost extends EntityPathBase<LostPost> {

    private static final long serialVersionUID = 1548912346L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLostPost lostPost = new QLostPost("lostPost");

    public final com.patrol.global.jpa.QBaseEntity _super = new com.patrol.global.jpa.QBaseEntity(this);

    public final com.patrol.domain.member.member.entity.QMember author;

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Double> latitude = createNumber("latitude", Double.class);

    public final StringPath location = createString("location");

    public final NumberPath<Double> longitude = createNumber("longitude", Double.class);

    public final DateTimePath<java.time.LocalDateTime> lostTime = createDateTime("lostTime", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final StringPath ownerPhone = createString("ownerPhone");

    public final NumberPath<Long> petId = createNumber("petId", Long.class);

    public final EnumPath<LostPost.Status> status = createEnum("status", LostPost.Status.class);

    public final StringPath tags = createString("tags");

    public final StringPath title = createString("title");

    public QLostPost(String variable) {
        this(LostPost.class, forVariable(variable), INITS);
    }

    public QLostPost(Path<? extends LostPost> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLostPost(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLostPost(PathMetadata metadata, PathInits inits) {
        this(LostPost.class, metadata, inits);
    }

    public QLostPost(Class<? extends LostPost> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.author = inits.isInitialized("author") ? new com.patrol.domain.member.member.entity.QMember(forProperty("author"), inits.get("author")) : null;
    }

}

