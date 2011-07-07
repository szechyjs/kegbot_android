package com.goliathonline.kegbot.util;

import com.goliathonline.kegbot.io.JsonHandler;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProvider;
import android.net.Uri;
import android.text.format.Time;

import java.io.InputStream;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Various utility methods used by {@link JsonHandler} implementations.
 */
public class ParserUtils {
    // TODO: consider refactor to HandlerUtils?

    // TODO: localize this string at some point
    public static final String BLOCK_TITLE_BREAKOUT_SESSIONS = "Breakout sessions";

    public static final String BLOCK_TYPE_FOOD = "food";
    public static final String BLOCK_TYPE_SESSION = "session";
    public static final String BLOCK_TYPE_OFFICE_HOURS = "officehours";

    // TODO: factor this out into a separate data file.
    public static final Set<String> LOCAL_TRACK_IDS = Sets.newHashSet(
            "accessibility", "android", "appengine", "chrome", "commerce", "developertools",
            "gamedevelopment", "geo", "googleapis", "googleapps", "googletv", "techtalk",
            "webgames", "youtube");

    /** Used to sanitize a string to be {@link Uri} safe. */
    private static final Pattern sSanitizePattern = Pattern.compile("[^a-z0-9-_]");
    private static final Pattern sParenPattern = Pattern.compile("\\(.*?\\)");

    /** Used to split a comma-separated string. */
    private static final Pattern sCommaPattern = Pattern.compile("\\s*,\\s*");

    private static Time sTime = new Time();

    /**
     * Sanitize the given string to be {@link Uri} safe for building
     * {@link ContentProvider} paths.
     */
    public static String sanitizeId(String input) {
        return sanitizeId(input, false);
    }

    /**
     * Sanitize the given string to be {@link Uri} safe for building
     * {@link ContentProvider} paths.
     */
    public static String sanitizeId(String input, boolean stripParen) {
        if (input == null) return null;
        if (stripParen) {
            // Strip out all parenthetical statements when requested.
            input = sParenPattern.matcher(input).replaceAll("");
        }
        return sSanitizePattern.matcher(input.toLowerCase()).replaceAll("");
    }

    /**
     * Split the given comma-separated string, returning all values.
     */
    public static String[] splitComma(CharSequence input) {
        if (input == null) return new String[0];
        return sCommaPattern.split(input);
    }

    /**
     * Build and return a new {@link JSONObject} with the given
     * {@link InputStream} assigned to it.
     */
    public static JSONObject newPullParser(InputStream input) throws JSONException {

        final JSONObject parser = new JSONObject(input.toString());
        return parser;
    }

    /**
     * Parse the given string as a RFC 3339 timestamp, returning the value as
     * milliseconds since the epoch.
     */
    public static long parseTime(String time) {
        sTime.parse3339(time);
        return sTime.toMillis(false);
    }

    /**
     * Translate an incoming {@link Tracks#TRACK_ID}, usually passing directly
     * through, but returning a different value when a local alias is defined.
     */
    public static String translateTrackIdAlias(String trackId) {
        //if ("gwt".equals(trackId)) {
        //    return "googlewebtoolkit";
        //} else {
        return trackId;
        //}
    }

    /**
     * Translate a possibly locally aliased {@link Tracks#TRACK_ID} to its real value;
     * this usually is a pass-through.
     */
    public static String translateTrackIdAliasInverse(String trackId) {
        //if ("googlewebtoolkit".equals(trackId)) {
        //    return "gwt";
        //} else {
        return trackId;
        //}
    }

    /** XML tag constants used by the Atom standard. */
    public interface AtomTags {
        String ENTRY = "entry";
        String UPDATED = "updated";
        String TITLE = "title";
        String LINK = "link";
        String CONTENT = "content";

        String REL = "rel";
        String HREF = "href";
    }
}
