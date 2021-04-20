package com.redis.demo.ddd;

import java.io.Serializable;

import static java.util.Objects.nonNull;

/**
 * @author hupeng.net@hotmail.com
 */
public interface Entity<T extends Entity, ID> extends Serializable {
    ID getId();

    default boolean sameIdentityAs(T other) {
        return nonNull(other) && nonNull(getId()) && getId().equals(other.getId());
    }
}
