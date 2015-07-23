package com.github.rvesse.airline.model;

import com.github.rvesse.airline.Accessor;
import com.github.rvesse.airline.utils.AirlineUtils;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;

public class ArgumentsMetadata {
    private final List<String> titles;
    private final String description, usage, completionCommand;
    private final int completionBehaviour;
    private final boolean required;
    private final Set<Accessor> accessors;
    private final int arity;

    public ArgumentsMetadata(Iterable<String> titles, String description, String usage, boolean required, int arity,
            int completionBehaviour, String completionCommand, Iterable<Field> path) {
        if (titles == null)
            throw new NullPointerException("title cannot be null");
        if (path == null)
            throw new NullPointerException("path cannot be null");
        if (!path.iterator().hasNext())
            throw new IllegalArgumentException("path cannot be empty");

        this.titles = ListUtils.unmodifiableList(IteratorUtils.toList(titles.iterator()));
        this.description = description;
        this.usage = usage;
        this.required = required;
        this.arity = arity <= 0 ? Integer.MIN_VALUE : arity;
        this.completionBehaviour = completionBehaviour;
        this.completionCommand = completionCommand;
        this.accessors = SetUtils.unmodifiableSet(AirlineUtils.singletonSet(new Accessor(path)));
    }

    public ArgumentsMetadata(Iterable<ArgumentsMetadata> arguments) {
        if (arguments == null)
            throw new NullPointerException("arguments cannot be null");
        if (!arguments.iterator().hasNext())
            throw new IllegalArgumentException("arguments cannot be empty");

        ArgumentsMetadata first = arguments.iterator().next();

        this.titles = first.titles;
        this.description = first.description;
        this.usage = first.usage;
        this.required = first.required;
        this.arity = first.arity;
        this.completionBehaviour = first.completionBehaviour;
        this.completionCommand = first.completionCommand;

        Set<Accessor> accessors = new HashSet<>();
        for (ArgumentsMetadata other : arguments) {
            if (!first.equals(other))
                throw new IllegalArgumentException(String.format("Conflicting arguments definitions: %s, %s", first,
                        other));

            accessors.addAll(other.getAccessors());
        }
        this.accessors = SetUtils.unmodifiableSet(accessors);
    }

    public List<String> getTitle() {
        return titles;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public boolean isRequired() {
        return required;
    }

    public int getArity() {
        return arity;
    }

    public int getCompletionBehaviours() {
        return completionBehaviour;
    }

    public String getCompletionCommand() {
        return completionCommand;
    }

    public Set<Accessor> getAccessors() {
        return accessors;
    }

    public boolean isMultiValued() {
        return accessors.iterator().next().isMultiValued();
    }

    public Class<?> getJavaType() {
        return accessors.iterator().next().getJavaType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArgumentsMetadata that = (ArgumentsMetadata) o;

        if (required != that.required) {
            return false;
        }
        if (description != null ? !description.equals(that.description) : that.description != null) {
            return false;
        }
        if (!titles.equals(that.titles)) {
            return false;
        }
        if (usage != null ? !usage.equals(that.usage) : that.usage != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = titles.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (usage != null ? usage.hashCode() : 0);
        result = 31 * result + (required ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ArgumentsMetadata");
        sb.append("{title='").append(StringUtils.join(titles, ',')).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", usage='").append(usage).append('\'');
        sb.append(", required=").append(required);
        sb.append(", accessors=").append(accessors);
        sb.append('}');
        return sb.toString();
    }
}
