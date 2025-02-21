package com.patrol.domain.comment.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QComment is a Querydsl query type for Comment
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QComment extends EntityPathBase<Comment> {

    private static final long serialVersionUID = 1332754232L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QComment comment = new QComment("comment");

    public final com.patrol.global.jpa.QBaseEntity _super = new com.patrol.global.jpa.QBaseEntity(this);

    public final com.patrol.domain.member.member.entity.QMember author;

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final com.patrol.domain.lostfound.entity.QFindPost findPost;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final com.patrol.domain.lostpost.entity.QLostPost lostPost;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public QComment(String variable) {
        this(Comment.class, forVariable(variable), INITS);
    }

    public QComment(Path<? extends Comment> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QComment(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QComment(PathMetadata metadata, PathInits inits) {
        this(Comment.class, metadata, inits);
    }

    public QComment(Class<? extends Comment> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.author = inits.isInitialized("author") ? new com.patrol.domain.member.member.entity.QMember(forProperty("author"), inits.get("author")) : null;
        this.findPost = inits.isInitialized("findPost") ? new com.patrol.domain.lostfound.entity.QFindPost(forProperty("findPost"), inits.get("findPost")) : null;
        this.lostPost = inits.isInitialized("lostPost") ? new com.patrol.domain.lostpost.entity.QLostPost(forProperty("lostPost")) : null;
    }

}

