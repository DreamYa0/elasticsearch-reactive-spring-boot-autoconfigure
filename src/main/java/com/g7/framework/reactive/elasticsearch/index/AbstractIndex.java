package com.g7.framework.reactive.elasticsearch.index;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.Date;

/**
 * @author dreamyao
 * @title 表公共基础字段
 * @date 2022/03/04 下午14:43
 * @since 1.0.0
 */
public abstract class AbstractIndex<T extends Serializable> implements Persistable<T> {

    // 主键ID
    @Id private @Nullable T id;
    // 创建时间
    @CreatedDate
    @Field(value = "gmtCreate", type = FieldType.Date, format = DateFormat.date_optional_time)
    private Date gmtCreate;
    // 更新时间
    @LastModifiedDate
    @Field(name = "gmtModified", type = FieldType.Date, format = DateFormat.date_optional_time)
    private Date gmtModified;

    @Nullable
    @Override
    public T getId() {
        return id;
    }

    public void setId(@Nullable T id) {
        this.id = id;
    }

    @Nullable
    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(@Nullable Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    @Nullable
    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(@Nullable Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    @Transient
    @Override
    public boolean isNew() {
        return null == getId();
    }
}
