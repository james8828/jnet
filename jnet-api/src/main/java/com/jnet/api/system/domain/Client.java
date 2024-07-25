package com.jnet.api.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @TableName sys_client
 */
@TableName(value ="sys_client")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Client implements Serializable {
    /**
     * 
     */
    @TableId
    private String clientRegistrationId;

    private String principalName;

    private String accessTokenType;

    private Date accessTokenIssuedAt;

    private Date accessTokenExpiresAt;

    /**
     * 
     */
    private String accessTokenScopes;

    /**
     * 
     */
    private Date refreshTokenIssuedAt;

    /**
     * 
     */
    private Date createdAt;

    /**
     * 
     */
    private Boolean enabled;

    /**
     * 
     */
    private Long createBy;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Long updateBy;

    /**
     * 
     */
    private Date updateTime;

    /**
     * 
     */
    private byte[] accessTokenValue;

    /**
     * 
     */
    private byte[] refreshTokenValue;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        Client other = (Client) that;
        return (this.getClientRegistrationId() == null ? other.getClientRegistrationId() == null : this.getClientRegistrationId().equals(other.getClientRegistrationId()))
            && (this.getPrincipalName() == null ? other.getPrincipalName() == null : this.getPrincipalName().equals(other.getPrincipalName()))
            && (this.getAccessTokenType() == null ? other.getAccessTokenType() == null : this.getAccessTokenType().equals(other.getAccessTokenType()))
            && (this.getAccessTokenIssuedAt() == null ? other.getAccessTokenIssuedAt() == null : this.getAccessTokenIssuedAt().equals(other.getAccessTokenIssuedAt()))
            && (this.getAccessTokenExpiresAt() == null ? other.getAccessTokenExpiresAt() == null : this.getAccessTokenExpiresAt().equals(other.getAccessTokenExpiresAt()))
            && (this.getAccessTokenScopes() == null ? other.getAccessTokenScopes() == null : this.getAccessTokenScopes().equals(other.getAccessTokenScopes()))
            && (this.getRefreshTokenIssuedAt() == null ? other.getRefreshTokenIssuedAt() == null : this.getRefreshTokenIssuedAt().equals(other.getRefreshTokenIssuedAt()))
            && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()))
            && (this.getEnabled() == null ? other.getEnabled() == null : this.getEnabled().equals(other.getEnabled()))
            && (this.getCreateBy() == null ? other.getCreateBy() == null : this.getCreateBy().equals(other.getCreateBy()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateBy() == null ? other.getUpdateBy() == null : this.getUpdateBy().equals(other.getUpdateBy()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getClientRegistrationId() == null) ? 0 : getClientRegistrationId().hashCode());
        result = prime * result + ((getPrincipalName() == null) ? 0 : getPrincipalName().hashCode());
        result = prime * result + ((getAccessTokenType() == null) ? 0 : getAccessTokenType().hashCode());
        result = prime * result + ((getAccessTokenIssuedAt() == null) ? 0 : getAccessTokenIssuedAt().hashCode());
        result = prime * result + ((getAccessTokenExpiresAt() == null) ? 0 : getAccessTokenExpiresAt().hashCode());
        result = prime * result + ((getAccessTokenScopes() == null) ? 0 : getAccessTokenScopes().hashCode());
        result = prime * result + ((getRefreshTokenIssuedAt() == null) ? 0 : getRefreshTokenIssuedAt().hashCode());
        result = prime * result + ((getCreatedAt() == null) ? 0 : getCreatedAt().hashCode());
        result = prime * result + ((getEnabled() == null) ? 0 : getEnabled().hashCode());
        result = prime * result + ((getCreateBy() == null) ? 0 : getCreateBy().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateBy() == null) ? 0 : getUpdateBy().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", clientRegistrationId=").append(clientRegistrationId);
        sb.append(", principalName=").append(principalName);
        sb.append(", accessTokenType=").append(accessTokenType);
        sb.append(", accessTokenIssuedAt=").append(accessTokenIssuedAt);
        sb.append(", accessTokenExpiresAt=").append(accessTokenExpiresAt);
        sb.append(", accessTokenScopes=").append(accessTokenScopes);
        sb.append(", refreshTokenIssuedAt=").append(refreshTokenIssuedAt);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", enabled=").append(enabled);
        sb.append(", createBy=").append(createBy);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateBy=").append(updateBy);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", accessTokenValue=").append(accessTokenValue);
        sb.append(", refreshTokenValue=").append(refreshTokenValue);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}