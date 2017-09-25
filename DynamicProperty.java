package com.avocarrot.json2view;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.Base64;
import android.view.ViewGroup;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Created by avocarrot on 11/12/2014.
 * Every Property of a View is a Dynaic Property
 */
class DynamicProperty {

    /**
     * possible types that we handle
     **/
    enum TYPE {
        NO_VALID,
        STRING,
        DIMEN,
        INTEGER,
        FLOAT,
        COLOR,
        COLOR_REF,
        REF,
        BOOLEAN,
        BASE64,
        DRAWABLE,
        JSON
    }

    /**
     * possible property name that we handle
     **/
    enum NAME {
        NO_VALID,
        ID,
        LAYOUT_WIDTH,
        LAYOUT_HEIGHT,
        PADDING_LEFT,
        PADDING_RIGHT,
        PADDING_TOP,
        PADDING_BOTTOM,
        PADDING,
        LAYOUT_MARGINLEFT,
        LAYOUT_MARGINRIGHT,
        LAYOUT_MARGINTOP,
        LAYOUT_MARGINBOTTOM,
        LAYOUT_MARGIN,
        BACKGROUND,
        ENABLED,
        SELECTED,
        CLICKABLE,
        SCALEX,
        SCALEY,
        MINWIDTH,
        MINHEIGTH,
        VISIBILITY,
        ONCLICK,
        /* textView */
        TEXT,
        TEXTCOLOR,
        TEXTSIZE,
        TEXTSTYLE,
        TEXTALLCAPS,
        ELLIPSIZE,
        MAXLINES,
        GRAVITY,
        DRAWABLETOP,
        DRAWABLEBOTTOM,
        DRAWABLELEFT,
        DRAWABLERIGHT,
        /* imageView */
        SRC,
        SCALETYPE,
        ADJUSTVIEWBOUNDS,
        /* layout */
        LAYOUT_ABOVE,
        LAYOUT_ALIGNBASELINE,
        LAYOUT_ALIGNBOTTOM,
        LAYOUT_ALIGNEND,
        LAYOUT_ALIGNLEFT,
        LAYOUT_ALIGNPARENTBOTTOM,
        LAYOUT_ALIGNPARENTEND,
        LAYOUT_ALIGNPARENTLEFT,
        LAYOUT_ALIGNPARENTRIGHT,
        LAYOUT_ALIGNPARENTSTART,
        LAYOUT_ALIGNPARENTTOP,
        LAYOUT_ALIGNRIGHT,
        LAYOUT_ALIGNSTART,
        LAYOUT_ALIGNTOP,
        LAYOUT_ALIGNWITHPARENTIFMISSING,
        LAYOUT_BELOW,
        LAYOUT_CENTERHORIZONTAL,
        LAYOUT_CENTERINPARENT,
        LAYOUT_CENTERVERTICAL,
        LAYOUT_TOENDOF,
        LAYOUT_TOLEFTOF,
        LAYOUT_TORIGHTOF,
        LAYOUT_TOSTARTOF,
        LAYOUT_GRAVITY,
        LAYOUT_WEIGHT,
        SUM_WEIGHT,
        ORIENTATION,

        TAG,
        FUNCTION
    }

    public NAME name;
    public TYPE type;
    private Object value;

    /**
     * @param v value to convert as string
     * @return Value as object depends on the type
     */
    private Object convertValue(Object v) {
        if (v==null)
            return null;
        switch (type) {
            case INTEGER: {
                return Integer.parseInt(v.toString());
            }
            case FLOAT: {
                return Float.parseFloat(v.toString());
            }
            case DIMEN: {
                return  convertDimenToPixel(v.toString());
            }
            case COLOR: {
                return convertColor(v.toString());
            }
            case BOOLEAN: {
                String value = v.toString();
                if (value.equalsIgnoreCase("t")) {
                    return true;
                } else if (value.equalsIgnoreCase("f")) {
                    return false;
                } else if (value.equalsIgnoreCase("true")) {
                    return true;
                } else if (value.equalsIgnoreCase("false")) {
                    return false;
                }
                return Integer.parseInt(value) == 1;
            }
            case BASE64: {
                try {
                    InputStream stream = new ByteArrayInputStream(Base64.decode(v.toString(), Base64.DEFAULT));
                    return BitmapFactory.decodeStream(stream);
                }
                catch (Exception e) {
                    return null;
                }
            }
            case DRAWABLE: {
                JSONObject drawableProperties = (JSONObject)v;

                GradientDrawable gd = new GradientDrawable();

                try {
                    gd.setColor ( convertColor( drawableProperties.getString("COLOR") ) );
                } catch (JSONException ignored) {}

                if (drawableProperties.has("CORNER")) {
                    String cornerValues = null;
                    try {
                        cornerValues = drawableProperties.getString("CORNER");
                    } catch (JSONException ignored){}
                    if (!TextUtils.isEmpty(cornerValues)) {
                        if (cornerValues.contains("|")) {
                            float[] corners = new float[8];
                            Arrays.fill(corners, 0);
                            String[] values = cornerValues.split("\\|");
                            int count = Math.min(values.length, corners.length);
                            for (int i=0 ; i<count ; i++) {
                                try {
                                    corners[i] = convertDimenToPixel(values[i]);
                                } catch (Exception e) {
                                    corners[i] = 0f;
                                }
                            }
                            gd.setCornerRadii(corners);
                        } else {
                            try {
                                gd.setCornerRadius( convertDimenToPixel(cornerValues) );
                            } catch (Exception e) {
                                gd.setCornerRadius(0f);
                            }
                        }
                    }

                }
                int strokeColor = 0x00FFFFFF;
                int strokeSize = 0;
                if (drawableProperties.has("STROKECOLOR")) {
                    try {
                        strokeColor = convertColor( drawableProperties.getString("STROKECOLOR") );
                    } catch (JSONException ignored) {}
                }
                if (drawableProperties.has("STROKESIZE")) {
                    try {
                        strokeSize = (int) convertDimenToPixel( drawableProperties.getString("STROKESIZE") );
                    } catch (JSONException ignored) {}
                }
                gd.setStroke(strokeSize, strokeColor);
                return gd;
            }
        }
        return v;
    }

    /**
     * create property and parse json
     * @param jsonObject : json to parse
     */
    DynamicProperty(JSONObject jsonObject) {
        super();
        try {
            name = NAME.valueOf(jsonObject.getString("name").toUpperCase().trim());
        } catch (Exception e) {
            name = NAME.NO_VALID;
        }
        try {
            type = TYPE.valueOf(jsonObject.getString("type").toUpperCase().trim());
        } catch (Exception e) {
            type = TYPE.NO_VALID;
        }
        try {
            value = convertValue(jsonObject.get("value"));
        } catch (Exception ignored) {}
    }

    public boolean isValid() {
        return value!=null;
    }

    /**
     * @param clazz :
     * @param varName :
     * @return search in clazz of possible variable name (varName) and return its value
     */
    Object getValueInt(Class clazz, String varName) {
        java.lang.reflect.Field fieldRequested;

        try {
            fieldRequested = clazz.getField(varName);
            if (fieldRequested!=null) {
                return fieldRequested.get(clazz);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** next function just cast value and return the object **/

    int getValueColor() {
        if (type == TYPE.COLOR) return Integer.class.cast(value);
        return -1;
    }

    String getValueString() {
        return String.class.cast(value);
    }

    int getValueInt() {
        if (value instanceof Integer)
            return Integer.class.cast(value);
        else if (value instanceof Float)
            return (int) getValueFloat();
        else
            return (int) value;
    }

    float getValueFloat() {
        return Float.class.cast(value);
    }

    Boolean getValueBoolean() {
        return Boolean.class.cast(value);
    }

    Bitmap getValueBitmap() {
        return (Bitmap)value;
    }

    Drawable getValueBitmapDrawable() {
        return new BitmapDrawable(Resources.getSystem(), getValueBitmap());
    }

    Drawable getValueGradientDrawable() {
        return (Drawable)value;
    }

    JSONObject getValueJSON() {
        return JSONObject.class.cast(value);
    }

    private int convertColor(String color) {
        if (color.startsWith("0x")) {
            return (int) Long.parseLong(color.substring(2), 16);
        }
        return Color.parseColor(color);
    }

    private float convertDimenToPixel(String dimen) {
        if (dimen.endsWith("dp"))
            return DynamicHelper.dpToPx(Float.parseFloat(dimen.substring(0, dimen.length() - 2)));
        else if (dimen.endsWith("sp"))
            return DynamicHelper.spToPx(Float.parseFloat(dimen.substring(0, dimen.length() - 2)));
        else if (dimen.endsWith("px"))
            return Integer.parseInt(dimen.substring(0, dimen.length() - 2));
        else if (dimen.endsWith("%"))
            return (int)(Float.parseFloat(dimen.substring(0, dimen.length() - 1))/100f * DynamicHelper.deviceWidth());
        else if (dimen.equalsIgnoreCase("match_parent"))
            return ViewGroup.LayoutParams.MATCH_PARENT;
        else if (dimen.equalsIgnoreCase("wrap_content"))
            return ViewGroup.LayoutParams.WRAP_CONTENT;
        else
            return Integer.parseInt(dimen);
    }

}
