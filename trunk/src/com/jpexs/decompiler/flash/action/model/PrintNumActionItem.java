/*
 *  Copyright (C) 2010-2014 PEXS
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jpexs.decompiler.flash.action.model;

import com.jpexs.decompiler.flash.SourceGeneratorLocalData;
import com.jpexs.decompiler.flash.action.model.operations.AddActionItem;
import com.jpexs.decompiler.flash.action.parser.script.ActionSourceGenerator;
import com.jpexs.decompiler.flash.action.swf4.ActionGetURL2;
import com.jpexs.decompiler.flash.helpers.GraphTextWriter;
import com.jpexs.decompiler.graph.GraphSourceItem;
import com.jpexs.decompiler.graph.GraphTargetItem;
import com.jpexs.decompiler.graph.SourceGenerator;
import com.jpexs.decompiler.graph.model.LocalData;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author JPEXS
 */
public class PrintNumActionItem extends ActionItem {

    private final GraphTargetItem num;
    private final GraphTargetItem boundingBox;

    @Override
    public List<GraphTargetItem> getAllSubItems() {
        List<GraphTargetItem> ret = new ArrayList<>();
        ret.add(num);
        ret.add(boundingBox);
        return ret;
    }

    public PrintNumActionItem(GraphSourceItem instruction, GraphTargetItem num, GraphTargetItem boundingBox) {
        super(instruction, PRECEDENCE_PRIMARY);
        this.num = num;
        this.boundingBox = boundingBox;
    }

    @Override
    public GraphTextWriter appendTo(GraphTextWriter writer, LocalData localData) throws InterruptedException {
        writer.append("printNum(");
        num.toString(writer, localData);
        writer.append(",");
        boundingBox.toString(writer, localData);
        return writer.append(")");
    }

    @Override
    public List<GraphSourceItem> toSource(SourceGeneratorLocalData localData, SourceGenerator generator) {
        ActionSourceGenerator asGenerator = (ActionSourceGenerator) generator;
        return toSourceMerge(localData, generator, new AddActionItem(src, asGenerator.pushConstTargetItem("print:#"), boundingBox, true), new AddActionItem(src, asGenerator.pushConstTargetItem("_level"), num, true), new ActionGetURL2(0, false, false));
    }

    @Override
    public boolean hasReturnValue() {
        return false;
    }
}
