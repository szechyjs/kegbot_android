package com.goliathonline.kegbot.io;

import com.goliathonline.kegbot.io.JsonHandler.HandlerException;
import com.goliathonline.kegbot.util.ParserUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;

import java.io.IOException;
import java.io.InputStream;

/**
 * Opens a local {@link Resources#getXml(int)} and passes the resulting
 * {@link XmlPullParser} to the given {@link JsonHandler}.
 */
public class LocalExecutor {
    private Resources mRes;
    private ContentResolver mResolver;

    public LocalExecutor(Resources res, ContentResolver resolver) {
        mRes = res;
        mResolver = resolver;
    }

    public void execute(Context context, String assetName, JsonHandler handler)
            throws HandlerException {
        try {
            final InputStream input = context.getAssets().open(assetName);
            final JSONObject parser = ParserUtils.newPullParser(input);
            handler.parseAndApply(parser, mResolver);
        } catch (HandlerException e) {
            throw e;
        } catch (JSONException e) {
            throw new HandlerException("Problem parsing local asset: " + assetName, e);
        } catch (IOException e) {
            throw new HandlerException("Problem parsing local asset: " + assetName, e);
        }
    }

    public void execute(int resId, JsonHandler handler) throws HandlerException {
        final XmlResourceParser parser = mRes.getXml(resId);
        try {
            //handler.parseAndApply(parser, mResolver);
        } finally {
            parser.close();
        }
    }
}
