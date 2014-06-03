/* 
 Copyright (C) GridGain Systems. All Rights Reserved.
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.examples.datagrid.hibernate;

import javax.persistence.*;
import java.util.*;

/**
 * An entity class representing a post, that a
 * {@link User} has made on some public service.
 */
@Entity
class Post {
    /** ID. */
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private long id;

    /** Author. */
    @ManyToOne
    private User author;

    /** Text. */
    private String text;

    /** Created timestamp. */
    private Date created;

    /**
     * Default constructor (required by Hibernate).
     */
    Post() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param author Author.
     * @param text Text.
     */
    Post(User author, String text) {
        this.author = author;
        this.text = text;
        created = new Date();
    }

    /**
     * @return ID.
     */
    public long getId() {
        return id;
    }

    /**
     * @param id New ID.
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return Author.
     */
    public User getAuthor() {
        return author;
    }

    /**
     * @param author New author.
     */
    public void setAuthor(User author) {
        this.author = author;
    }

    /**
     * @return Text.
     */
    public String getText() {
        return text;
    }

    /**
     * @param text New text.
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return Created timestamp.
     */
    public Date getCreated() {
        return (Date)created.clone();
    }

    /**
     * @param created New created timestamp.
     */
    public void setCreated(Date created) {
        this.created = (Date)created.clone();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return "Post [id=" + id +
            ", text=" + text +
            ", created=" + created +
            ']';
    }
}
