package io.airlift.airline.parser;

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import io.airlift.airline.Context;
import io.airlift.airline.TypeConverter;
import io.airlift.airline.model.ArgumentsMetadata;
import io.airlift.airline.model.CommandGroupMetadata;
import io.airlift.airline.model.CommandMetadata;
import io.airlift.airline.model.GlobalMetadata;
import io.airlift.airline.model.OptionMetadata;

import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.base.Predicates.compose;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.Iterables.find;

public class Parser {
    private static final Pattern SHORT_OPTIONS_PATTERN = Pattern.compile("-[^-].*");

    // global> (option value*)* (group (option value*)*)? (command (option
    // value* | arg)* '--'? args*)?
    public ParseState parse(GlobalMetadata metadata, String... params) {
        return parse(metadata, ImmutableList.copyOf(params));
    }

    public ParseState parse(GlobalMetadata metadata, Iterable<String> params) {
        PeekingIterator<String> tokens = Iterators.peekingIterator(params.iterator());

        ParseState state = ParseState.newInstance().pushContext(Context.GLOBAL).withGlobal(metadata);

        // parse global options
        state = parseOptions(tokens, state, metadata.getOptions());

        // parse group
        if (tokens.hasNext()) {
            Predicate<? super CommandGroupMetadata> findGroupPredicate;
            //@formatter:off
            findGroupPredicate = metadata != null && metadata.allowsAbbreviatedCommands() 
                                 ? new AbbreviatedGroupFinder(tokens.peek(), metadata.getCommandGroups()) 
                                 : compose(equalTo(tokens.peek()), CommandGroupMetadata.nameGetter());
            //@formatter:on
            CommandGroupMetadata group = find(metadata.getCommandGroups(), findGroupPredicate, null);
            if (group != null) {
                tokens.next();
                state = state.withGroup(group).pushContext(Context.GROUP);

                state = parseOptions(tokens, state, state.getGroup().getOptions());
            }
        }

        // parse command
        List<CommandMetadata> expectedCommands = metadata.getDefaultGroupCommands();
        if (state.getGroup() != null) {
            expectedCommands = state.getGroup().getCommands();
        }

        if (tokens.hasNext()) {
            Predicate<? super CommandMetadata> findCommandPredicate;
            //@formatter:off
            findCommandPredicate = metadata != null && metadata.allowsAbbreviatedCommands() 
                                   ? new AbbreviatedCommandFinder(tokens.peek(), expectedCommands)
                                   : compose(equalTo(tokens.peek()), CommandMetadata.nameGetter());
            //@formatter:on
            CommandMetadata command = find(expectedCommands, findCommandPredicate, state.getGroup() != null ? state
                    .getGroup().getDefaultCommand() : null);

            boolean usingDefault = false;
            if (command == null && state.getGroup() == null && metadata.getDefaultCommand() != null) {
                usingDefault = true;
                command = metadata.getDefaultCommand();
            }

            if (command == null) {
                while (tokens.hasNext()) {
                    state = state.withUnparsedInput(tokens.next());
                }
            } else {
                if (tokens.peek().equals(command.getName()) || (!usingDefault && metadata.allowsAbbreviatedCommands())) {
                    tokens.next();
                }

                state = state.withCommand(command).pushContext(Context.COMMAND);

                while (tokens.hasNext()) {
                    state = parseOptions(tokens, state, command.getCommandOptions());

                    state = parseArgs(state, tokens, command.getArguments(), command.getDefaultOption());
                }
            }
        }

        return state;
    }

    public ParseState parseCommand(CommandMetadata command, Iterable<String> params) {
        PeekingIterator<String> tokens = Iterators.peekingIterator(params.iterator());
        ParseState state = ParseState.newInstance().pushContext(Context.GLOBAL).withCommand(command);

        while (tokens.hasNext()) {
            state = parseOptions(tokens, state, command.getCommandOptions());

            state = parseArgs(state, tokens, command.getArguments(), command.getDefaultOption());
        }
        return state;
    }

    private ParseState parseOptions(PeekingIterator<String> tokens, ParseState state,
            List<OptionMetadata> allowedOptions) {
        while (tokens.hasNext()) {
            //
            // Try to parse next option(s) using different styles. If code
            // matches it returns the next parser state, otherwise it returns null.

            // Parse a simple option
            ParseState nextState = parseSimpleOption(tokens, state, allowedOptions);
            if (nextState != null) {
                state = nextState;
                continue;
            }

            // Parse GNU getopt long-form: --option=value
            nextState = parseLongGnuGetOpt(tokens, state, allowedOptions);
            if (nextState != null) {
                state = nextState;
                continue;
            }

            // Handle classic getopt syntax: -abc
            nextState = parseClassicGetOpt(tokens, state, allowedOptions);
            if (nextState != null) {
                state = nextState;
                continue;
            }

            // did not match an option
            break;
        }

        return state;
    }

    private ParseState parseSimpleOption(PeekingIterator<String> tokens, ParseState state,
            List<OptionMetadata> allowedOptions) {
        OptionMetadata option = findOption(state, allowedOptions, tokens.peek());
        if (option == null) {
            return null;
        }

        tokens.next();
        state = state.pushContext(Context.OPTION).withOption(option);

        Object value;
        if (option.getArity() == 0) {
            state = state.withOptionValue(option, Boolean.TRUE).popContext();
        } else if (option.getArity() == 1) {
            if (tokens.hasNext()) {
                String tokenStr = tokens.next();
                checkValidValue(option, tokenStr);
                value = TypeConverter.newInstance().convert(option.getTitle(), option.getJavaType(), tokenStr);
                state = state.withOptionValue(option, value).popContext();
            }
        } else {
            ImmutableList.Builder<Object> values = ImmutableList.builder();

            int count = 0;

            boolean hasSeparator = false;
            boolean foundNextOption = false;
            while (count < option.getArity() && tokens.hasNext() && !hasSeparator) {
                String peekedToken = tokens.peek();
                hasSeparator = peekedToken.equals("--");
                foundNextOption = findOption(state, allowedOptions, peekedToken) != null;

                if (hasSeparator || foundNextOption)
                    break;
                String tokenStr = tokens.next();
                checkValidValue(option, tokenStr);
                values.add(TypeConverter.newInstance().convert(option.getTitle(), option.getJavaType(), tokenStr));
                ++count;
            }

            if (count == option.getArity() || hasSeparator || foundNextOption) {
                state = state.withOptionValue(option, values.build()).popContext();
            }
        }
        return state;
    }

    private ParseState parseLongGnuGetOpt(PeekingIterator<String> tokens, ParseState state,
            List<OptionMetadata> allowedOptions) {
        List<String> parts = ImmutableList.copyOf(Splitter.on('=').limit(2).split(tokens.peek()));
        if (parts.size() != 2) {
            return null;
        }

        OptionMetadata option = findOption(state, allowedOptions, parts.get(0));
        if (option == null || option.getArity() != 1) {
            // TODO: this is not exactly correct. It should be an error
            // condition
            return null;
        }

        // we have a match so consume the token
        tokens.next();

        // update state
        state = state.pushContext(Context.OPTION).withOption(option);
        checkValidValue(option, parts.get(1));
        Object value = TypeConverter.newInstance().convert(option.getTitle(), option.getJavaType(), parts.get(1));
        state = state.withOption(option).withOptionValue(option, value).popContext();

        return state;
    }

    private ParseState parseClassicGetOpt(PeekingIterator<String> tokens, ParseState state,
            List<OptionMetadata> allowedOptions) {
        if (!SHORT_OPTIONS_PATTERN.matcher(tokens.peek()).matches()) {
            return null;
        }

        // remove leading dash from token
        String remainingToken = tokens.peek().substring(1);

        ParseState nextState = state;
        while (!remainingToken.isEmpty()) {
            char tokenCharacter = remainingToken.charAt(0);

            // is the current token character a single letter option?
            OptionMetadata option = findOption(state, allowedOptions, "-" + tokenCharacter);
            if (option == null) {
                return null;
            }

            nextState = nextState.pushContext(Context.OPTION).withOption(option);

            // remove current token character
            remainingToken = remainingToken.substring(1);

            // for no argument options, process the option and remove the
            // character from the token
            if (option.getArity() == 0) {
                nextState = nextState.withOptionValue(option, Boolean.TRUE).popContext();
                continue;
            }

            if (option.getArity() == 1) {
                // we must, consume the current token so we can see the next
                // token
                tokens.next();

                // if current token has more characters, this is the value;
                // otherwise it is the next token
                if (!remainingToken.isEmpty()) {
                    checkValidValue(option, remainingToken);
                    Object value = TypeConverter.newInstance().convert(option.getTitle(), option.getJavaType(),
                            remainingToken);
                    nextState = nextState.withOptionValue(option, value).popContext();
                } else if (tokens.hasNext()) {
                    String tokenStr = tokens.next();
                    checkValidValue(option, tokenStr);
                    Object value = TypeConverter.newInstance().convert(option.getTitle(), option.getJavaType(),
                            tokenStr);
                    nextState = nextState.withOptionValue(option, value).popContext();
                }

                return nextState;
            }

            throw new UnsupportedOperationException("Short options style can not be used with option "
                    + option.getAllowedValues());
        }

        // consume the current token
        tokens.next();

        return nextState;
    }

    /**
     * Checks for a valid value and throws an error if the value for the option
     * is restricted and not in the set of allowed values
     * 
     * @param option
     *            Option meta data
     * @param tokenStr
     *            Token string
     */
    private void checkValidValue(OptionMetadata option, String tokenStr) {
        if (option.getAllowedValues() == null)
            return;
        if (option.getAllowedValues().contains(tokenStr))
            return;
        throw new ParseOptionIllegalValueException(option.getTitle(), tokenStr, option.getAllowedValues());
    }

    private ParseState parseArgs(ParseState state, PeekingIterator<String> tokens, ArgumentsMetadata arguments,
            OptionMetadata defaultOption) {
        if (tokens.hasNext()) {
            if (tokens.peek().equals("--")) {
                state = state.pushContext(Context.ARGS);
                tokens.next();

                // Consume all remaining tokens as arguments
                // Default option can't possibly apply at this point because we
                // saw the -- separator
                while (tokens.hasNext()) {
                    state = parseArg(state, tokens, arguments, null);
                }
            } else {
                state = parseArg(state, tokens, arguments, defaultOption);
            }
        }

        return state;
    }

    private ParseState parseArg(ParseState state, PeekingIterator<String> tokens, ArgumentsMetadata arguments,
            OptionMetadata defaultOption) {
        if (arguments != null) {
            // Enforce maximum arity on arguments
            if (arguments.getArity() > 0 && state.getParsedArguments().size() == arguments.getArity()) {
                throw new ParseTooManyArgumentsException(
                        "Too many arguments, at most %d arguments are permitted but extra argument %s was encountered",
                        arguments.getArity(), tokens.peek());
            }

            // Argument
            state = state.withArgument(TypeConverter.newInstance().convert(arguments.getTitle().get(0),
                    arguments.getJavaType(), tokens.next()));
        } else if (defaultOption != null) {
            // Default Option
            state = state.withOption(defaultOption);
            String tokenStr = tokens.next();
            checkValidValue(defaultOption, tokenStr);
            Object value = TypeConverter.newInstance().convert(defaultOption.getTitle(), defaultOption.getJavaType(), tokenStr);
            state = state.withOptionValue(defaultOption, value).popContext();
        } else {
            // Unparsed input
            state = state.withUnparsedInput(tokens.next());
        }
        return state;
    }

    private OptionMetadata findOption(ParseState state, List<OptionMetadata> options, String param) {
        Predicate<? super OptionMetadata> findOptionPredicate;
        //if (state.getGlobal() != null && state.getGlobal().allowsAbbreviatedOptions()) {
        //    
        //}
        
        for (OptionMetadata optionMetadata : options) {
            if (optionMetadata.getOptions().contains(param)) {
                return optionMetadata;
            }
        }
        return null;
    }

}
