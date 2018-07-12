package org.eclipse.jdt.ls.core.internal;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.xtext.xbase.lib.Pure;

/**
 * @author Pixy Yuan
 * on 2018/7/12
 */
public class WorkspaceIdentifier {

    private String uri;

    public WorkspaceIdentifier() {
    }

    public WorkspaceIdentifier(String uri) {
        this.uri = uri;
    }

    @Pure
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Pure
    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("uri", uri)
            .toString();
    }

    @Pure
    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        WorkspaceIdentifier that = (WorkspaceIdentifier)o;

        return uri != null ? uri.equals(that.uri) : that.uri == null;
    }

    @Pure
    @Override
    public int hashCode() {
        return uri != null ? uri.hashCode() : 0;
    }
}
