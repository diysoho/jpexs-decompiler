/*
 *  Copyright (C) 2010-2015 JPEXS, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.jpexs.decompiler.flash.importers.svg;

import com.jpexs.decompiler.flash.importers.ShapeImporter;
import com.jpexs.decompiler.flash.tags.base.ImageTag;
import com.jpexs.helpers.Helper;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author JPEXS
 */
class SvgStyle implements Cloneable {

    public Color color;

    public SvgFill fill;

    public double opacity;

    public double fillOpacity;

    public SvgFill strokeFill;

    public Color stopColor;

    public double stopOpacity;

    public double strokeWidth;

    public double strokeOpacity;

    public SvgLineCap strokeLineCap;

    public SvgLineJoin strokeLineJoin;

    public double strokeMiterLimit;

    public SvgStyle parentStyle;

    private final SvgImporter importer;

    private final Random random = new Random();

    public SvgStyle(SvgImporter importer) {
        this.importer = importer;
        fill = new SvgColor(Color.black);
        fillOpacity = 1;
        strokeFill = null;
        strokeWidth = 1;
        strokeOpacity = 1;
        opacity = 1;
        stopOpacity = 1;
        stopColor = null;
        strokeLineCap = SvgLineCap.BUTT;
        strokeLineJoin = SvgLineJoin.MITER;
        strokeMiterLimit = 4;
    }

    public SvgStyle(SvgStyle parentStyle) {
        this(parentStyle.importer);
        this.parentStyle = parentStyle;
    }

    public SvgFill getFillWithOpacity() {
        if (fill == null) {
            return null;
        }
        if (!(fill instanceof SvgColor)) {
            return fill;
        }
        Color fillColor = ((SvgColor) fill).color;

        int opacity = (int) Math.round(this.opacity * fillOpacity * 255);
        if (opacity == 255) {
            return fill;
        }

        return new SvgColor(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), opacity);
    }

    public SvgFill getStrokeFillWithOpacity() {
        if (strokeFill == null) {
            return null;
        }
        if (!(strokeFill instanceof SvgColor)) {
            return strokeFill;
        }
        Color strokeFillColor = ((SvgColor) strokeFill).color;

        int opacity = (int) Math.round(this.opacity * strokeOpacity * 255);
        if (opacity == 255) {
            return strokeFill;
        }

        return new SvgColor(strokeFillColor.getRed(), strokeFillColor.getGreen(), strokeFillColor.getBlue(), opacity);
    }

    public SvgFill getStrokeColorWithOpacity() {
        if (strokeFill == null) {
            return null;
        }
        if (!(strokeFill instanceof SvgColor)) {
            return strokeFill;
        }

        Color strokeColor = ((SvgColor) strokeFill).color;

        int opacity = (int) Math.round(this.opacity * strokeOpacity * 255);
        if (opacity == 255) {
            return strokeFill;
        }

        return new SvgColor(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(), opacity);
    }

    @Override
    public SvgStyle clone() {
        try {
            SvgStyle ret = (SvgStyle) super.clone();
            return ret;
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException();
        }
    }

    //FIXME - matrices
    private SvgFill parseGradient(Map<String, Element> idMap, Element el, SvgStyle style) {
        SvgGradientUnits gradientUnits = null;
        String gradientTransform = null;
        SvgSpreadMethod spreadMethod = null;
        SvgInterpolation interpolation = null;

        String x1 = null;
        String y1 = null;
        String x2 = null;
        String y2 = null;

        String cx = null;
        String cy = null;
        String fx = null;
        String fy = null;
        String r = null;

        List<SvgStop> stops = new ArrayList<>();
        //inheritance:
        if (el.hasAttribute("xlink:href")) {
            String parent = el.getAttribute("xlink:href");
            if (parent.startsWith("#")) {
                String parentId = parent.substring(1);
                Element parent_el = idMap.get(parentId);
                if (parent_el == null) {
                    importer.showWarning("fillNotSupported", "Parent gradient not found.");
                    return new SvgColor(random.nextInt(256), random.nextInt(256), random.nextInt(256));
                }

                if ("linearGradient".equals(el.getTagName()) && parent_el.getTagName().equals(el.getTagName())) {
                    SvgLinearGradient parentFill = (SvgLinearGradient) parseGradient(idMap, parent_el, style);
                    gradientUnits = parentFill.gradientUnits;
                    gradientTransform = parentFill.gradientTransform;
                    spreadMethod = parentFill.spreadMethod;

                    x1 = parentFill.x1;
                    y1 = parentFill.y1;
                    x2 = parentFill.x2;
                    y2 = parentFill.y2;
                    interpolation = parentFill.interpolation;
                    stops = parentFill.stops;
                }
                if ("radialGradient".equals(el.getTagName()) && parent_el.getTagName().equals(el.getTagName())) {
                    SvgRadialGradient parentFill = (SvgRadialGradient) parseGradient(idMap, parent_el, style);
                    gradientUnits = parentFill.gradientUnits;
                    gradientTransform = parentFill.gradientTransform;
                    spreadMethod = parentFill.spreadMethod;

                    cx = parentFill.cx;
                    cy = parentFill.cy;
                    fx = parentFill.fx;
                    fy = parentFill.fy;
                    r = parentFill.r;
                    interpolation = parentFill.interpolation;
                    stops = parentFill.stops;
                }

            } else {
                importer.showWarning("fillNotSupported", "Parent gradient invalid.");
                return new SvgColor(random.nextInt(256), random.nextInt(256), random.nextInt(256));
            }
        }

        if (el.hasAttribute("gradientUnits")) {
            switch (el.getAttribute("gradientUnits")) {
                case "userSpaceOnUse":
                    gradientUnits = SvgGradientUnits.USER_SPACE_ON_USE;
                    break;
                case "objectBoundingBox":
                    gradientUnits = SvgGradientUnits.OBJECT_BOUNDING_BOX;
                    break;
                default:
                    importer.showWarning("fillNotSupported", "Unsupported  gradientUnits: " + el.getAttribute("gradientUnits"));
                    return new SvgColor(random.nextInt(256), random.nextInt(256), random.nextInt(256));
            }

        }
        if (el.hasAttribute("gradientTransform")) {
            gradientTransform = el.getAttribute("gradientTransform");
        }
        if (el.hasAttribute("spreadMethod")) {
            switch (el.getAttribute("spreadMethod")) {
                case "pad":
                    spreadMethod = SvgSpreadMethod.PAD;
                    break;
                case "reflect":
                    spreadMethod = SvgSpreadMethod.REFLECT;
                    break;
                case "repeat":
                    spreadMethod = SvgSpreadMethod.REPEAT;
                    break;
            }
        }
        if (el.hasAttribute("x1")) {
            x1 = el.getAttribute("x1").trim();
        }
        if (el.hasAttribute("y1")) {
            y1 = el.getAttribute("y1").trim();
        }
        if (el.hasAttribute("x2")) {
            x2 = el.getAttribute("x2").trim();
        }
        if (el.hasAttribute("y2")) {
            y2 = el.getAttribute("y2").trim();
        }

        if (el.hasAttribute("cx")) {
            cx = el.getAttribute("cx").trim();
        }
        if (el.hasAttribute("cy")) {
            cy = el.getAttribute("cy").trim();
        }
        if (el.hasAttribute("fx")) {
            fx = el.getAttribute("fx").trim();
        }
        if (el.hasAttribute("fy")) {
            fy = el.getAttribute("fy").trim();
        }
        if (el.hasAttribute("r")) {
            r = el.getAttribute("r").trim();
        }
        if (el.hasAttribute("color-interpolation") && interpolation == null) { //prefer inherit
            switch (el.getAttribute("color-interpolation")) {
                case "sRGB":
                    interpolation = SvgInterpolation.SRGB;
                    break;
                case "linearRGB":
                    interpolation = SvgInterpolation.LINEAR_RGB;
                    break;
                case "auto": //without preference, put SRGB there
                    interpolation = SvgInterpolation.SRGB;
                    break;
            }
        }
        if (interpolation == null) {
            interpolation = SvgInterpolation.SRGB;
        }

        if (gradientUnits == null) {
            gradientUnits = SvgGradientUnits.OBJECT_BOUNDING_BOX;
        }
        if (spreadMethod == null) {
            spreadMethod = SvgSpreadMethod.PAD;
        }

        if (x1 == null) {
            x1 = "0%";
        }
        if (y1 == null) {
            y1 = "0%";
        }

        if (x2 == null) {
            x2 = "100%";
        }
        if (y2 == null) {
            y2 = "0%";
        }

        if (cx == null) {
            cx = "50%";
        }
        if (cy == null) {
            cy = "50%";
        }

        if (r == null) {
            r = "50%";
        }
        if (fx == null) {
            fx = cx;
        }
        if (fy == null) {
            fy = cy;
        }

        NodeList stopNodes = el.getElementsByTagName("stop");
        boolean stopsCleared = false;
        for (int i = 0; i < stopNodes.getLength(); i++) {
            Node node = stopNodes.item(i);
            if (node instanceof Element) {
                Element stopEl = (Element) node;
                SvgStyle newStyle = style.apply(stopEl, idMap);

                String offsetStr = stopEl.getAttribute("offset");
                double offset;
                if (offsetStr.endsWith("%")) {
                    offset = Double.parseDouble(offsetStr.substring(0, offsetStr.length() - 1)) / 100;
                } else {
                    offset = Double.parseDouble(offsetStr);
                }
                Color color = newStyle.stopColor;
                if (color == null) {
                    color = Color.BLACK;
                }

                int alpha = (int) Math.round(newStyle.stopOpacity * 255);
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                if (!stopsCleared) { //It has some stop nodes -> remove all inherited stops
                    stopsCleared = true;
                    stops = new ArrayList<>();
                }
                stops.add(new SvgStop(color, offset));
            }
        }

        if ("linearGradient".equals(el.getTagName())) {
            SvgLinearGradient ret = new SvgLinearGradient();
            ret.x1 = x1;
            ret.y1 = y1;
            ret.x2 = x2;
            ret.y2 = y2;
            ret.spreadMethod = spreadMethod;
            ret.gradientTransform = gradientTransform;
            ret.gradientUnits = gradientUnits;
            ret.stops = stops;
            ret.interpolation = interpolation;
            return ret;
        } else if ("radialGradient".equals(el.getTagName())) {
            SvgRadialGradient ret = new SvgRadialGradient();
            ret.cx = cx;
            ret.cy = cy;
            ret.fx = fx;
            ret.fy = fy;
            ret.r = r;
            ret.spreadMethod = spreadMethod;
            ret.gradientTransform = gradientTransform;
            ret.gradientUnits = gradientUnits;
            ret.stops = stops;
            ret.interpolation = interpolation;
            return ret;
        } else {
            return null;
        }
    }

    private Color parseColor(String rgbStr) {
        SvgFill fill = parseFill(new HashMap<>(), rgbStr, null);
        return fill.toColor();
    }

    private SvgFill parseFill(Map<String, Element> idMap, String rgbStr, SvgStyle style) {
        if (rgbStr == null) {
            return null;
        }

        Pattern idPat = Pattern.compile("url\\(#([^)]+)\\).*");
        java.util.regex.Matcher mPat = idPat.matcher(rgbStr);

        if (mPat.matches()) {
            String elementId = mPat.group(1);
            Element e = idMap.get(elementId);
            if (e != null) {
                String tagName = e.getTagName();
                if ("linearGradient".equals(tagName)) {
                    return parseGradient(idMap, e, new SvgStyle(style)); //? new style
                }

                if ("radialGradient".equals(tagName)) {
                    return parseGradient(idMap, e, new SvgStyle(style)); //? new style
                }

                if ("pattern".equals(tagName)) {
                    Element element = null;
                    NodeList childNodes = e.getChildNodes();
                    for (int i = 0; i < childNodes.getLength(); i++) {
                        if (childNodes.item(i) instanceof Element) {
                            if (element != null) {
                                element = null;
                                break;
                            }

                            element = (Element) childNodes.item(i);
                        }
                    }

                    if (element != null && "image".equals(element.getTagName())) {
                        String attr = element.getAttribute("xlink:href").trim();
                        // this is ugly, but supports the format which is exported by FFDec
                        if (attr.startsWith("data:image/") && attr.contains("base64,")) {
                            String base64 = attr.substring(attr.indexOf("base64,") + 7);
                            byte[] data = Helper.base64StringToByteArray(base64);
                            try {
                                ImageTag imageTag = new ShapeImporter().addImage(importer.shapeTag, data, 0);
                                SvgBitmapFill bitmapFill = new SvgBitmapFill();
                                bitmapFill.characterId = imageTag.characterID;
                                if (e.hasAttribute("patternTransform")) {
                                    bitmapFill.patternTransform = e.getAttribute("patternTransform");
                                }

                                return bitmapFill;
                            } catch (IOException ex) {
                                Logger.getLogger(ShapeImporter.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }

                importer.showWarning("fillNotSupported", "Unknown fill style. Random color assigned.");
                return new SvgColor(random.nextInt(256), random.nextInt(256), random.nextInt(256));
            }

            rgbStr = rgbStr.substring(elementId.length() + 6).trim(); // remove url(#...)
        }

        SvgColor result = SvgColor.parse(rgbStr);
        if (result == null) {
            importer.showWarning("fillNotSupported", "Unknown fill style. Random color assigned.");
            return new SvgColor(random.nextInt(256), random.nextInt(256), random.nextInt(256));
        }

        return result;
    }

    private void applyStyle(Map<String, Element> idMap, SvgStyle style, String name, String value) {
        if (value == null || value.length() == 0) {
            return;
        }

        switch (name) {
            case "color": {
                Color color = parseColor(value);
                if (color != null) {
                    style.color = color == SvgColor.TRANSPARENT ? null : color;
                }
            }
            break;
            case "fill": {
                SvgFill fill = parseFill(idMap, value, style);
                if (fill != null) {
                    style.fill = fill == SvgColor.SVG_TRANSPARENT ? null : fill;
                }
            }
            break;
            case "stop-color": {
                if ("currentColor".equals(value)) {
                    if (style.parentStyle != null) {
                        style.stopColor = style.parentStyle.color;
                    }
                } else if ("inherit".equals(value)) {
                    importer.showWarning(value + "StopColorNotSupported", "The stop color value '" + value + "' is not supported.");
                } else {
                    style.stopColor = parseColor(value);
                }
            }
            break;
            case "fill-opacity": {
                double opacity = Double.parseDouble(value);
                style.fillOpacity = opacity;
            }
            break;
            case "stop-opacity": {
                if ("inherit".equals(value)) {
                    importer.showWarning(value + "StopOpacityNotSupported", "The stop opacity value '" + value + "' is not supported.");
                } else {
                    double stopOpacity = Double.parseDouble(value);
                    style.stopOpacity = stopOpacity;
                }
            }
            break;
            case "stroke": {
                SvgFill strokeFill = parseFill(idMap, value, style);
                if (strokeFill != null) {
                    style.strokeFill = strokeFill == SvgColor.SVG_TRANSPARENT ? null : strokeFill;
                }
            }
            break;
            case "stroke-width": {
                double strokeWidth = Double.parseDouble(value);
                style.strokeWidth = strokeWidth;
            }
            break;
            case "stroke-opacity": {
                double opacity = Double.parseDouble(value);
                style.strokeOpacity = opacity;
            }
            break;
            case "stroke-linecap": {
                switch (value) {
                    case "butt":
                        style.strokeLineCap = SvgLineCap.BUTT;
                        break;
                    case "round":
                        style.strokeLineCap = SvgLineCap.ROUND;
                        break;
                    case "square":
                        style.strokeLineCap = SvgLineCap.SQUARE;
                        break;
                }
            }
            break;
            case "stroke-linejoin": {
                switch (value) {
                    case "miter":
                        style.strokeLineJoin = SvgLineJoin.MITER;
                        break;
                    case "round":
                        style.strokeLineJoin = SvgLineJoin.ROUND;
                        break;
                    case "bevel":
                        style.strokeLineJoin = SvgLineJoin.BEVEL;
                        break;
                }
            }
            break;
            case "stroke-miterlimit": {
                double strokeMiterLimit = Double.parseDouble(value);
                style.strokeMiterLimit = strokeMiterLimit;
            }
            case "opacity": {
                double opacity = Double.parseDouble(value);
                style.opacity = opacity;
            }
            break;
        }
    }

    public SvgStyle apply(Element element, Map<String, Element> idMap) {
        SvgStyle result = clone();

        String[] styles = new String[]{
            "color", "fill", "fill-opacity",
            "stroke", "stroke-width", "stroke-opacity", "stroke-linecap", "stroke-linejoin", "stroke-miterlimit",
            "opacity", "stop-color", "stop-opacity"
        };

        for (String style : styles) {
            if (element.hasAttribute(style)) {
                String attr = element.getAttribute(style).trim();
                applyStyle(idMap, result, style, attr);
            }
        }

        if (element.hasAttribute("style")) {
            String[] styleDefs = element.getAttribute("style").split(";");
            for (String styleDef : styleDefs) {
                String[] parts = styleDef.split(":", 2);
                applyStyle(idMap, result, parts[0].trim(), parts[1].trim());
            }
        }

        return result;
    }
}
