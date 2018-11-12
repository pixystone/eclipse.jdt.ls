package org.eclipse.jdt.ls.core.internal.handlers;

/**
 * @author Pixy Yuan
 * on 2018/11/12
 */
public class RenameClassFileParams {
	private String currentUri;
	private String newName;

	public RenameClassFileParams(String currentUri, String newName) {
		this.currentUri = currentUri;
		this.newName = newName;
	}

	public String getCurrentUri() {
		return currentUri;
	}

	public void setCurrentUri(String currentUri) {
		this.currentUri = currentUri;
	}

	public String getNewName() {
		return newName;
	}

	public void setNewName(String newName) {
		this.newName = newName;
	}

	@Override
	public String toString() {
		return "ResourceRenameParams{" +
			"currentUri='" + currentUri + '\'' +
			", newName='" + newName + '\'' +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) { return true; }
		if (o == null || getClass() != o.getClass()) { return false; }

		RenameClassFileParams that = (RenameClassFileParams)o;

		if (!currentUri.equals(that.currentUri)) { return false; }
		return newName.equals(that.newName);
	}

	@Override
	public int hashCode() {
		int result = currentUri.hashCode();
		result = 31 * result + newName.hashCode();
		return result;
	}
}
