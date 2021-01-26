
package com.tesshu.jpsonic.service.search.analysis;

import java.util.Map;

import com.tesshu.jpsonic.service.search.analysis.ComplementaryFilter.Mode;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

public class ComplementaryFilterFactory extends TokenFilterFactory {

    private Mode mode;
    private String stopwards;

    public ComplementaryFilterFactory(Map<String, String> args) {
        super(args);
        stopwards = get(args, "stopwards");
        String maybeMode = get(args, "mode");
        Mode.fromValue(maybeMode).ifPresent(m -> this.mode = m);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public ComplementaryFilter create(TokenStream input) {
        return new ComplementaryFilter(input, mode, stopwards);
    }

}
