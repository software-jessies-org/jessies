package e.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.text.*;
import java.util.Map;
import javax.swing.*;

/**
 * Originally a copy of:
 *
 * http://weblogs.java.net/blog/zixle/archive/2006/06/modern_heap_vie.html
 * 
 * Looking a gift horse in the mouth:
 * 
 * I removed the blurry text because it just made the text harder to read, and
 * created 300 KiB/s of garbage on Mac OS.
 * 
 * I honored the user's desktop anti-aliasing settings, so the text doesn't
 * look terrible.
 * 
 * I removed the pop-up menu because it looked terrible and didn't offer
 * anything useful.
 * 
 * I disabled the grid drawing because both the horizontal and the vertical
 * grid lines were meaningless.
 * 
 * I changed "MB" to "MiB".
 * 
 * I fixed all the spelling mistakes.
 * 
 * FIXME: a meaningful grid might be useful.
 * FIXME: it would be nice to be able to point to any line in the graph and see what the heap size/utilization was at that time. (and maybe how many seconds ago that was.)
 * FIXME: the colors are ugly.
 * FIXME: we shouldn't forget all our data on resize.
 * 
 * @author Scott Violet
 * @author Elliott Hughes
 */
public class HeapView extends JComponent {
    /*
     * How often the display is updated.
     */
    private static final int TICK = 1000;
    
    /**
     * Time (in ms) to animate heap growing.
     */
    private static final int HEAP_GROW_ANIMATE_TIME = 1000;
    
    /**
     * Width of the border.
     */
    private static final int BORDER_W = 2;
    
    /**
     * Height of the border area.
     */
    private static final int BORDER_H = 4;
    
    /**
     * Colors for the grid. This is alternating pairs for a linear gradient.
     */
    private static final Color[] GRID_COLORS = new Color[] {
        new Color(0xE3DFCF), new Color(0xE7E4D3),
        new Color(0xDAD7C6), new Color(0xDFDCCB),
        new Color(0xD3CFBF), new Color(0xD7D3C3),
        new Color(0xCECABA), new Color(0xD0CCBC)
    };
    
    /**
     * Border color.
     */
    private static final Color BORDER1_COLOR = new Color(0xA6A295);

    /**
     * Border color.
     */
    private static final Color BORDER2_COLOR = new Color(0xC0BCAD);
    
    /**
     * Start color for the tick gradient.
     */
    private static final Color MIN_TICK_COLOR = new Color(0xC7D6AD);

    /**
     * End color for the tick gradient.
     */
    private static final Color MAX_TICK_COLOR = new Color(0x615d0f);

    /**
     * Start color for the background gradient.
     */
    private static final Color BACKGROUND1_COLOR = new Color(0xD0CCBC);

    /**
     * End color for the background gradient.
     */
    private static final Color BACKGROUND2_COLOR = new Color(0xEAE7D7);

    /**
     * MessageFormat used to generate text.
     */
    private final MessageFormat format;
    
    /**
     * Data for the graph as a percentage of the heap used.
     */
    private float[] graph;
    
    /**
     * Index into graph for the next tick.
     */
    private int graphIndex;
    
    /**
     * If true, graph contains all valid data, otherwise valid data starts at
     * 0 and ends at graphIndex - 1.
     */
    private boolean graphFilled;
    
    /**
     * Last total heap size.
     */
    private long lastTotal;
    
    /**
     * Timer used to update data.
     */
    private Timer updateTimer;
    
    /**
     * Image containing the background gradient and tiles.
     */
    private Image bgImage;
    
    /**
     * Width data is cached at.
     */
    private int cachedWidth;

    /**
     * Height data is cached at.
     */
    private int cachedHeight;
    
    /**
     * Image containing text.
     */
    private BufferedImage textImage;
    
    /**
     * Timer used to animate heap size growing.
     */
    private HeapGrowTimer heapGrowTimer;
    
    /**
     * Current text being displayed.
     */
    private String heapSizeText;

    /**
     * Image containing gradient for ticks.
     */
    private Image tickGradientImage;

    /**
     * Image drawn on top of the ticks.
     */
    private BufferedImage gridOverlayImage;
    
    public HeapView() {
        format = new MessageFormat("{0,number,0.0}/{1,number,0.0} MiB");
        heapSizeText = "";
        updateUI();
    }
    
    /**
     * Updates the look and feel for this component.
     */
    @Override
    public void updateUI() {
        setFont(UIManager.getFont("Label.font"));
        setOpaque(true);
        revalidate();
        repaint();
    }
    
    /**
     * Returns the minimum size.
     *
     * @return the minimum size
     */
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        return getPreferredSize0();
    }
    
    /**
     * Returns the preferred size.
     *
     * @return the preferred size
     */
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        return getPreferredSize0();
    }
    
    private Dimension getPreferredSize0() {
        return new Dimension(100, getFontMetrics(getFont()).getHeight() + 8);
    }

    /**
     * Returns the first index to start rendering from.
     */
    private int getGraphStartIndex() {
        if (graphFilled) {
            return graphIndex;
        } else {
            return 0;
        }
    }

    /**
     * Paints the component.
     */
    protected void paintComponent(Graphics oldGraphics) {
        Graphics2D g = (Graphics2D) oldGraphics;
        // Get the desktop rendering hints so that if the user's chosen anti-aliased text, we give it to them.
        Map map = (Map) (Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints"));
        if (map != null) {
            g.addRenderingHints(map);
        }
        
        int width = getWidth();
        int height = getHeight();
        if (width - BORDER_W > 0 && height - BORDER_H > 0) {
            startTimerIfNecessary();
            updateCacheIfNecessary(width, height);
            paintCachedBackground(g, width, height);
            g.translate(1, 2);
            int innerW = width - BORDER_W;
            int innerH = height - BORDER_H;
            if (heapGrowTimer != null) {
                // Render the heap growing animation.
                Composite lastComposite = g.getComposite();
                float percent = 1f - heapGrowTimer.getPercent();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, percent));
                g.drawImage(heapGrowTimer.image, 0, 0, null);
                g.setComposite(lastComposite);
            }
            paintTicks(g, innerW, innerH);
            // FIXME(enh): if the grid weren't meaningless, it might be worth drawing.
            //g.drawImage(getGridOverlayImage(), 0, 0, null);
            paintText(g, innerW, innerH);
            g.translate(-1, -2);
        } else {
            stopTimerIfNecessary();
            // To honor opaque contract, fill in the background
            g.setColor(getBackground());
            g.fillRect(0, 0, width, height);
        }
    }
    
    private void paintTicks(Graphics2D g, int width, int height) {
        int numCells = GRID_COLORS.length / 2;
        int cellSize = (height - numCells - 1) / numCells;
        
        if (graphIndex > 0 || graphFilled) {
            int index = getGraphStartIndex();
            int x = 0;
            if (!graphFilled) {
                x = width - graphIndex;
            }
            float min = graph[index];
            index = (index + 1) % graph.length;
            while (index != graphIndex) {
                min = Math.min(min, graph[index]);
                index = (index + 1) % graph.length;
            }
            int minHeight = (int)(min * (float)height);
            if (minHeight > 0) {
               g.drawImage(tickGradientImage, x, height - minHeight, width, height,
                        x, height - minHeight, width, height, null);
            }
            index = getGraphStartIndex();
            do {
                int tickHeight = (int)(graph[index] * (float)height);
                if (tickHeight > minHeight) {
                    g.drawImage(tickGradientImage, x, height - tickHeight, x + 1, height - minHeight,
                            x, height - tickHeight, x + 1, height - minHeight, null);
                }
                index = (index + 1) % graph.length;
                x++;
            } while (index != graphIndex);
        }
    }

    /**
     * Renders the text.
     */
    private void paintText(Graphics g, int w, int h) {
        g.setFont(getFont());
        g.drawString(heapSizeText, 8, h - g.getFontMetrics().getAscent());
    }
    
    /**
     * Paints the grid on top of the ticks.
     */
    private void paintGridOverlay(Graphics2D g, int w, int h) {
        int numCells = GRID_COLORS.length / 2;
        int cellSize = (h - numCells - 1) / numCells;
        int c1 = 0xD0CCBC;
        int c2 = 0xEAE7D7;
        g.setPaint(new GradientPaint(0, 0, new Color((c1 >> 16) & 0xFF, (c1 >> 8) & 0xFF, c1 & 0xFF, 0x30), 0, h, new Color((c2 >> 16) & 0xFF, (c2 >> 8) & 0xFF, c2 & 0xFF, 0x40)));
        g.setColor(Color.GRAY);
        for (int x = 0; x < w; x += cellSize + 1) {
            g.fillRect(x, 0, 1, h);
        }
        for (int y = h - cellSize - 1; y >= 0; y -= (cellSize + 1)) {
            g.fillRect(0, y, w, 1);
        }
    }

    private void paintCachedBackground(Graphics2D g, int w, int h) {
        if (bgImage != null) {
            g.drawImage(bgImage, 0, 0, null);
        }
    }
    
    private void paintBackgroundTiles(Graphics2D g, int w, int h) {
        g.translate(1, 2);
        w -= BORDER_W;
        h -= BORDER_H;
        int numCells = GRID_COLORS.length / 2;
        int cellSize = (h - numCells - 1) / numCells;
        for (int i = 0; i < numCells; i++) {
            int colorIndex = i;
            int y = h - cellSize * (i + 1) - i;
            int x = 1;
            g.setPaint(new GradientPaint(0, y, GRID_COLORS[colorIndex * 2],
                    0, y + cellSize - 1, GRID_COLORS[colorIndex * 2 + 1]));
            while (x < w) {
                int endX = Math.min(w, x + cellSize);
                g.fillRect(x, y, endX - x, cellSize);
                x = endX + 1;
            }
            y += cellSize + 1;
        }
        g.translate(-1, -2);
    }
    
    private void paintBackground(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(0, 0, BACKGROUND1_COLOR,
                0, h, BACKGROUND2_COLOR));
        g.fillRect(0, 0, w, h);
    }
    
    private void paintBorder(Graphics g, int w, int h) {
        // Draw the border
        g.setColor(BORDER1_COLOR);
        g.drawRect(0, 0, w - 1, h - 2);
        g.setColor(BORDER2_COLOR);
        g.fillRect(1, 1, w - 2, 1);
        g.setColor(Color.WHITE);
        g.fillRect(0, h - 1, w, 1);
    }
    
    private void updateCacheIfNecessary(int w, int h) {
        if (cachedWidth != w || cachedHeight != h) {
            cachedWidth = w;
            cachedHeight = h;
            updateCache(w, h);
        }
    }
    
    private Image getGridOverlayImage() {
        if (gridOverlayImage == null) {
            gridOverlayImage = new BufferedImage(
                    getInnerWidth(), getInnerHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = gridOverlayImage.createGraphics();
            paintGridOverlay(g, getInnerWidth(), getInnerHeight());
            g.dispose();
        }
        return gridOverlayImage;
    }

    /**
     * Recreates the various state information needed for rendering.
     */
    private void updateCache(int w, int h) {
        disposeImages();
        textImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        bgImage = createImage(w, h);
        Graphics2D imageG = (Graphics2D)bgImage.getGraphics();
        paintBackground(imageG, w, h);
        // FIXME(enh): if the grid weren't meaningless, it might be worth drawing.
        //paintBackgroundTiles(imageG, w, h);
        paintBorder(imageG, w, h);
        imageG.dispose();
        w -= BORDER_W;
        h -= BORDER_H;
        if (graph == null || graph.length != w) {
            graph = new float[w];
            graphFilled = false;
            graphIndex = 0;
        }
        GradientPaint tickGradient = new GradientPaint(0, h, MIN_TICK_COLOR,
                w, 0, MAX_TICK_COLOR);
        tickGradientImage = createImage(w, h);
        imageG = (Graphics2D)tickGradientImage.getGraphics();
        imageG.setPaint(tickGradient);
        imageG.fillRect(0, 0, w, h);
        imageG.dispose();
        if (gridOverlayImage != null) {
            gridOverlayImage.flush();
            gridOverlayImage = null;
        }
    }
    
    /**
     * Invoked when component removed from a heavy weight parent. Stops the
     * timer.
     */
    public void removeNotify() {
        super.removeNotify();
        stopTimerIfNecessary();
    }
    
    /**
     * Restarts the timer.
     */
    private void startTimerIfNecessary() {
        if (updateTimer == null) {
            updateTimer = new Timer(TICK, new ActionHandler());
            updateTimer.setRepeats(true);
            updateTimer.start();
        }
    }
    
    /**
     * Stops the timer.
     */
    private void stopTimerIfNecessary() {
        if (updateTimer != null) {
            graph = null;
            graphFilled = false;
            updateTimer.stop();
            updateTimer = null;
            lastTotal = 0;
            disposeImages();
            cachedHeight = cachedHeight = -1;
            if (heapGrowTimer != null) {
                heapGrowTimer.stop();
                heapGrowTimer = null;
            }
        }
    }

    private void disposeImages() {
        if (bgImage != null) {
            bgImage.flush();
            bgImage = null;
        }
        if (textImage != null) {
            textImage.flush();
            textImage = null;
        }
        if (tickGradientImage != null) {
            tickGradientImage.flush();
            tickGradientImage = null;
        }
        if (gridOverlayImage != null) {
            gridOverlayImage.flush();
            gridOverlayImage = null;
        }
    }
    
    /**
     * Invoked when the update timer fires. Updates the necessary data
     * structures and triggers repaints.
     */
    private void update() {
        if (!isShowing()) {
            // Either we've become invisible, or one of our ancestors has.
            // Stop the timer and bale. Next paint will trigger timer to
            // restart.
            stopTimerIfNecessary();
            return;
        }
        Runtime r = Runtime.getRuntime();
        long total = r.totalMemory();
        if (total != lastTotal) {
            if (lastTotal != 0) {
                // Total heap size has changed, start an animation.
                startHeapAnimate();
                // Readjust the graph size based on the new max.
                int index = getGraphStartIndex();
                do {
                    graph[index] = (float)(((double)graph[index] *
                            (double)lastTotal) / (double)total);
                    index = (index + 1) % graph.length;
                } while (index != graphIndex);
            }
            lastTotal = total;
        }
        if (heapGrowTimer == null) {
            // Not animating a heap size change, update the graph data and text.
            long used = total - r.freeMemory();
            graph[graphIndex] = (float)((double)used / (double)total);
            graphIndex = (graphIndex + 1) % graph.length;
            if (graphIndex == 0) {
                graphFilled = true;
            }
            heapSizeText = format.format(
                    new Object[] { new Double((double)used / 1024 / 1024),
                                   new Double((double)total / 1024 / 1024) });
        }
        repaint();
    }
    
    private void startHeapAnimate() {
        if (heapGrowTimer == null) {
            heapGrowTimer = new HeapGrowTimer();
            heapGrowTimer.start();
        }
    }
    
    private void stopHeapAnimate() {
        if (heapGrowTimer != null) {
            heapGrowTimer.stop();
            heapGrowTimer = null;
        }
    }

    private int getInnerWidth() {
        return getWidth() - BORDER_W;
    }

    private int getInnerHeight() {
        return getHeight() - BORDER_H;
    }

    
    private final class ActionHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            update();
        }
    }
    
    
    private final class HeapGrowTimer extends Timer {
        private final long startTime;
        private float percent;
        BufferedImage image;
        
        HeapGrowTimer() {
            super(30, null);
            setRepeats(true);
            startTime = System.currentTimeMillis();
            percent = 0f;
            int w = getWidth() - BORDER_W;
            int h = getHeight() - BORDER_H;
            image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            paintTicks(g, w, h);
            g.dispose();
        }
        
        public float getPercent() {
            return percent;
        }
        
        protected void fireActionPerformed(ActionEvent e) {
            long time = System.currentTimeMillis();
            long delta = time - startTime;
            if (delta > HEAP_GROW_ANIMATE_TIME) {
                stopHeapAnimate();
            } else {
                percent = (float)delta / (float)HEAP_GROW_ANIMATE_TIME;
                repaint();
            }
        }
    }
}
