package com.g7.framework.reactive.elasticsearch.index;

import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;

/**
 * @author dreamyao
 * @title
 * @date 2022/03/04 下午14:43
 * @since 1.0.0
 */
public abstract class AbstractVersionIndex<T extends Serializable> extends AbstractIndex<T> {

    // 版本，使用做乐观锁用
    @Version
    @Field(name = "version",type = FieldType.Integer)
    private Long version;

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
