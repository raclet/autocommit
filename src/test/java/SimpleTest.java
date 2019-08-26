import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AutoCommitTestWatcher.class)
class SimpleTest {

    private final static int NB_TEST = 3;
    private final static String QUESTION = "Q1";

    @BeforeAll
    public static void setUp() {
        System.setProperty("nbTest", Integer.toString(NB_TEST));
        System.setProperty("question", QUESTION);
    }

    @Test
    public void fTest1() {
        assertEquals(0, Simple.f(0,0));
    }

    @Test
    public void fTest2() {
        assertEquals(-1, Simple.f(1,0));
    }

    @Test
    public void fTest3() {
        assertEquals(1, Simple.f(-1,0));
    }

}