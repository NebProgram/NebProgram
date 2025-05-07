package ru.kos.neb_viewer;

import java.awt.*;
import java.util.Random;

import org.piccolo2d.nodes.PPath;


public class FlashColor implements Runnable
{
  Thread m_AnimateTask = null;
  public long time1 = 100;
  public long time2 = 100;

  private final PPath node;
  private final Color color1;
  private final Color color2;

  private final Random random = new Random();

  public FlashColor(PPath node, Color color1, Color color2)
  {
//      System.out.println("Set up flashing");
      this.color1=color1;
      this.color2=color2;
      this.node=node;
  }

  @Override
  @SuppressWarnings("SleepWhileInLoop")
    public void run()
    {
        while (!Main.stop_flash_running)
        {
            double dolja=Math.abs(random.nextDouble());
            node.setPaint(color1);
            node.repaint();
            try { Thread.sleep((long)(time1*dolja)); } catch (InterruptedException _) {}
            node.setPaint(color2);
            node.repaint();
            try { Thread.sleep((long)(time2*dolja)); } catch (InterruptedException _) {}

        }
    }

    public void start()
    {
        if (m_AnimateTask == null)
        {
            m_AnimateTask = new Thread(this);
            m_AnimateTask.start();
        }
    }

    public void stop()
    {
        Main.stop_flash_running = true;
    }
}
