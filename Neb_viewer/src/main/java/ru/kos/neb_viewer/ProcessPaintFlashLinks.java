package ru.kos.neb_viewer;

/*

  @author kos
 */
import org.piccolo2d.event.PInputEvent;


public class ProcessPaintFlashLinks implements Runnable
{
    Thread m_ProcessPaintFlashLinks = null;
    PInputEvent e;
    private final long timeout_position_edge=500;

    public ProcessPaintFlashLinks(PInputEvent e)
    {
        this.e=e;
    }
    @Override
    public void run()
    {
        try { Thread.sleep(timeout_position_edge); } catch (InterruptedException _) {}
        try
        {
            Utils.paintFlashLinks(e);
        }
        catch(java.lang.OutOfMemoryError | java.lang.NullPointerException _) {}
    }

    public void start()
    {
        if (m_ProcessPaintFlashLinks == null)
        {
//            System.out.println("ProcessPaintFlashLinks started.");
            m_ProcessPaintFlashLinks = new Thread(this);
            m_ProcessPaintFlashLinks.start();
        }
    }

    @SuppressWarnings("deprecation")
    public void stop()
    {
        Main.stop_flash_running = true;
    }
}

class ProcessHideFlashLinks implements Runnable
{
    Thread m_ProcessHideFlashLinks = null;
    public long timeout_flash_link = 30000;

    public ProcessHideFlashLinks()
    {
    }

    public ProcessHideFlashLinks(long timeout)
    {
        timeout_flash_link = timeout;
    }


    @Override
    public void run()
    {
        try
        {
            try { Thread.sleep(timeout_flash_link); } catch(java.lang.InterruptedException _) {}
            Utils.StopAllFlashLinkProcesses();
            Utils.hideFlashLinks();
        }
        catch(java.lang.OutOfMemoryError | java.lang.NullPointerException _) {}
    }

    public void start()
    {
        if (m_ProcessHideFlashLinks == null)
        {
//            System.out.println("Start process HideFlash.");
            m_ProcessHideFlashLinks = new Thread(this);
            m_ProcessHideFlashLinks.start();
        }
    }

    @SuppressWarnings("deprecation")
    public void stop()
    {
        Main.stop_flash_running = true;
    }


}

