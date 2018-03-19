/*
 * Copyright 2018 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataconservancy.pass.model;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The funder or sponsor of Grant or award.
 * @author Karen Hanson
 */

public class Funder extends PassEntity {

    /** 
     * String type name, specifically used to set "@type" in JSON serialization
     */
    @JsonProperty("@type")
    private String type = PassEntityType.FUNDER.getName();
    
    /** 
     * Funder name 
     */
    private String name;
    
    /** 
     * Funder URL 
     */
    private String url;
    
    /** 
     * URI of the Policy associated with funder
     */
    private URI policy;

    /** 
     * ID assigned to the funder within the researcher's institution 
     */
    private String localId;
    
    @Override
    public String getType() {
        return type;
    }

    
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    
    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    
    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    
    /**
     * @return the URI of the policy
     */
    public URI getPolicy() {
        return policy;
    }

    
    /**
     * @param policy the URI of the policy to set
     */
    public void setPolicy(URI policy) {
        this.policy = policy;
    }

    
    /**
     * @return the localId
     */
    public String getLocalId() {
        return localId;
    }

    
    /**
     * @param localId the localId to set
     */
    public void setLocalId(String localId) {
        this.localId = localId;
    }

    

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Funder that = (Funder) o;

        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        if (policy != null ? !policy.equals(that.policy) : that.policy != null) return false;
        if (localId != null ? !localId.equals(that.localId) : that.localId != null) return false;
        return true;
    }

    
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (policy != null ? policy.hashCode() : 0);
        result = 31 * result + (localId != null ? localId.hashCode() : 0);
        return result;
    }
    

}
