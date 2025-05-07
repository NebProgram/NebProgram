package ru.kos.neb_viewer;

interface ATerminal
{
    //final Style style = new Style();
    final class Style
    {
        final static short
                PLAIN = (short) 0,
                BOLD = (short) 1,
                ITALIC = (short) (1 << 1),
                INVERSE = (short) (1 << 2),
                UNDERLINE = (short) (1 << 3);
    }
    
    void setEmulator(AEmulator emu);
    
    void CR();
    void LF();
    void BS();
    void HT();
    
    void receive(char ch);
    void receive(char ch, int format);
    void send(char ch);

    int getNumRows();
    int getNumCols();
    
    int getCursorRow();
    int getCursorCol();
    
    void setCursorPos(int row, int col);
    
    void setCursorVisible(boolean state);
    boolean getCursorVisible();
    
    int getAreaTop();
    int getAreaBottom();
    int getAreaLeft();
    int getAreaRight();
    
    void resetArea();
    void setArea(int top, int bottom);
    void setArea(int top, int bottom, int left, int right);
    
    short getStyle();
    void setStyle(int style);
    
    short getForeColor();
    short getBackColor();
    void setForeColor(int color);
    void setBackColor(int color);
    
    void setChars(int left, int right, char ch, int style);
    void delChars(int left, int right, char ch, int style);
    void insChars(int left, int right, char ch, int style);
    
    void clearEOL(char ch, int style);
    void clearBOL(char ch, int style);
    void clearLine(char ch, int style);
    
    void clearEOD(char ch, int style);
    void clearBOD(char ch, int style);
    void clearAll(char ch, int style);
    
    void rollRegion(int top, int bottom, int amount);
    void rollScreen(int amount);
}