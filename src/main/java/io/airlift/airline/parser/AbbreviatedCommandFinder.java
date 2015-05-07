package io.airlift.airline.parser;

import java.util.Collection;
import io.airlift.airline.model.CommandMetadata;

public final class AbbreviatedCommandFinder extends AbstractAbbreviationFinder<CommandMetadata> {

    public AbbreviatedCommandFinder(String cmd, Collection<CommandMetadata> commands) {
        super(cmd, commands);
    }

    @Override
    protected boolean isExactNameMatch(String value, CommandMetadata item) {
        return item.getName().equals(value);
    }

    @Override
    protected boolean isPartialNameMatch(String value, CommandMetadata item) {
        return item.getName().startsWith(value);
    }
}
