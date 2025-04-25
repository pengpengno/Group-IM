package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext;

public class Indent
{
    double width = 15;
    int level = 1;

    Indent() {}

    Indent( int level )
    {
        if ( level > 0 ) this.level = level;
    }

    Indent increase()
    {
    	  return new Indent( level+1 );
    }

    Indent decrease()
    {
    	  return new Indent( level-1 );
    }

    int getLevel() { return level; }
    
    @Override
    public String toString()
    {
        return "indent: "+ level;
    }
}
