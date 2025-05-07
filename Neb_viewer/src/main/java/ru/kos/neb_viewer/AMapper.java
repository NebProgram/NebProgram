package ru.kos.neb_viewer;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

class AMapper extends KeyAdapter
{
    AEmulator emu;
    
    AMapper()
    {
    }
    
    @Override
    public void keyTyped(KeyEvent e)
    {
        char ch = e.getKeyChar();
        if (ch == '\n')
            ch = '\r';
        send(ch);
    }
    
    void send(char ch)
    {
        emu.send(ch);
    }
    
    public synchronized void send(String s)
    {
        for (int i = 0; i < s.length(); i++)
            send(s.charAt(i));
    }
}
