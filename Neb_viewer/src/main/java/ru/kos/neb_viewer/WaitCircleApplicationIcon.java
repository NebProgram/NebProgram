package ru.kos.neb_viewer;

import java.net.URL;

class WaitCircleApplicationIcon implements Runnable
{
    Thread m_WaitCircle = null;
    public boolean end=false;

    @Override
    @SuppressWarnings("SleepWhileInLoop")
    public void run()
    {
        // set icon wait ...
//        ImageIcon m_ImageIcon = new ImageIcon();
        URL[] url = new URL[12];
        url[0] = Main.class.getResource("/images/wait/wait_request_state1.png");
        url[1] = Main.class.getResource("/images/wait/wait_request_state2.png");
        url[2] = Main.class.getResource("/images/wait/wait_request_state3.png");
        url[3] = Main.class.getResource("/images/wait/wait_request_state4.png");
        url[4] = Main.class.getResource("/images/wait/wait_request_state5.png");
        url[5] = Main.class.getResource("/images/wait/wait_request_state6.png");
        url[6] = Main.class.getResource("/images/wait/wait_request_state7.png");
        url[7] = Main.class.getResource("/images/wait/wait_request_state8.png");
        url[8] = Main.class.getResource("/images/wait/wait_request_state9.png");
        url[9] = Main.class.getResource("/images/wait/wait_request_state10.png");
        url[10] = Main.class.getResource("/images/wait/wait_request_state11.png");
        url[11] = Main.class.getResource("/images/wait/wait_request_state12.png");
          
        while(!end)
        {
            for(int i=0; i<12; i++)
            {
                Main.getFrames()[0].setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(url[i]));
                try { Thread.sleep(100); } catch (InterruptedException _) {}
            }
            try { Thread.sleep(100); } catch (InterruptedException _) {}
        }
        // set default icon
        URL url_default = Main.class.getResource("/images/idle.gif");
        Main.getFrames()[0].setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(url_default));  
    }

    public void start()
    {
        if (m_WaitCircle == null)
        {
            m_WaitCircle = new Thread(this);
            m_WaitCircle.start();
        }
    }

    public void stop()
    {
        end = true;
    }

}
