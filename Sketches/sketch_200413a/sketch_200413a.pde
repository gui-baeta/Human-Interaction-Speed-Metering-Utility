// Bakeoff #2 - Seleção de Alvos e Fatores Humanos
// IPM 2019-20, Semestre 2
// Bake-off: durante a aula de lab da semana de 20 de Abril
// Submissão via Twitter: exclusivamente no dia 24 de Abril, até às 23h59

// Processing reference: https://processing.org/reference/

import java.util.Collections;
import processing.sound.*;

// Sound files
// sounds to add:
// hitSound: https://www.youtube.com/watch?v=g-lcamn3VRE
// hitSound: https://www.youtube.com/watch?v=qwvUvx9W8mA
// missSOund: https://www.youtube.com/watch?v=nbONxjyiVj4
// loseSound: https://www.youtube.com/watch?v=CQeezCdF4mk
// missSound: https://www.youtube.com/watch?v=Fw3RB7xnb80
// loseSound: https://www.youtube.com/watch?v=BCniqMTSecQ
SoundFile hitSound;
SoundFile missSound;
SoundFile winSound;
SoundFile loseSound;

// Target properties
float PPI, PPCM;
float SCALE_FACTOR;
float TARGET_SIZE;
float TARGET_PADDING, MARGIN, LEFT_PADDING, TOP_PADDING;
// Before!!
// final float PPI            = 227;            // http://dpi.lv/ (Macbook Pro 13 Retina: 227) - CHANGE TO YOUR DISPLAY PPI!
// final float PPCM           = PPI / 2.54;     // do not change this!
// final float TARGET_SIZE    = 1.5 * PPCM;     // set the target size in cm; do not change this!
// final float TARGET_PADDING   = 0.1 * PPCM;   // set the padding around the targets in cm; do not change this!
// final float MARGIN           = 1.5 * PPCM;   // set the margin around the targets in cm; do not change this!

// Study properties
ArrayList<Integer> trials  = new ArrayList<Integer>();    // contains the order of targets that activate in the test
int trialNum               = 0;                           // the current trial number (indexes into trials array above)
final int NUM_REPEATS      = 3;                           // sets the number of times each target repeats in the test - FOR THE BAKEOFF NEEDS TO BE 3!
boolean ended              = false;

// Performance variables
int startTime              = 0;      // time starts when the first click is captured
int finishTime             = 0;      // records the time of the final click
int hits                   = 0;      // number of successful clicks
int misses                 = 0;      // number of missed clicks

// Class used to store properties of a target
class Target {
    int x, y;
    float w;

    Target(int posx, int posy, float twidth) {
        x = posx;
        y = posy;
        // w = twidth + 0.7*PPCM; Before!!
        w = twidth;
    }
}

// Setup window and vars - runs once
void setup() {
    // size(700, 700);    // window size in px Before!!
    fullScreen();

    SCALE_FACTOR    = 1.0 / displayDensity();            // scale factor for high-density displays
    String[] ppi_string = loadStrings("ppi.txt");        // The text from the file is loaded into an array.
    PPI            = float(ppi_string[1]);               // set PPI, we assume the ppi value is in the second line of the .txt
    PPCM           = PPI / 2.54 * SCALE_FACTOR;          // do not change this!
    TARGET_SIZE    = 1.5 * PPCM;                         // set the target size in cm; do not change this!
    TARGET_PADDING = 1.5 * PPCM;                         // set the padding around the targets in cm; do not change this!
    MARGIN         = 1.5 * PPCM;                         // set the margin around the targets in cm; do not change this!
    LEFT_PADDING   = width/2 - TARGET_SIZE - 1.5*TARGET_PADDING - 1.5*MARGIN;        // set the margin of the grid of targets to the left of the canvas; do not change this!
    TOP_PADDING    = height/2 - TARGET_SIZE - 1.5*TARGET_PADDING - 1.5*MARGIN;       // set the margin of the grid of targets to the top of the canvas; do not change this!

    noStroke();        // draw shapes without outlines
    frameRate(60);     // set frame rate

    // Text and font setup
    textFont(createFont("Source Code Pro Black", 16));    // sets the font to Arial size 16
    textAlign(CENTER);                    // align text

    randomizeTrials();    // randomize the trial order for each participant

    hitSound = new SoundFile(this, "hit.wav");
    missSound = new SoundFile(this, "burp.wav");
    winSound = new SoundFile(this, "complete_shorter.wav");
    loseSound = new SoundFile(this, "mario_death.wav");

    hitSound.amp(1);
    missSound.amp(0.01);
    winSound.amp(0.1);
    loseSound.amp(0.1);
}

// Updates UI - this method is constantly being called and drawing targets
void draw() {

    if (hasEnded()) {
        return;            // nothing else to do; study is over
    }

    background(15);       // set background to black

    // Print trial count
    fill(100);          // set text fill color to white
    text("Trial " + (trialNum + 1) + " of " + trials.size(), 70, 20);    // display what trial the participant is on (the top-left corner)

    // Draw targets
    for (int i = 0; i < 16; i++) {
        drawTarget(i);
    }
}

boolean hasEnded() {
    if (ended) {
        return true; // returns if test has ended before
    }

    // Check if the study is over
    if (trialNum >= trials.size()) {
        float timeTaken = (finishTime-startTime) / 1000f;     // convert to seconds - DO NOT CHANGE!
        float penalty = constrain(((95f-((float)hits*100f/(float)(hits+misses)))*.2f),0,100);    // calculate penalty - DO NOT CHANGE!

        printResults(timeTaken, penalty);    // prints study results on-screen
        ended = true;
    }

    return ended;
}

// Randomize the order in the targets to be selected
// DO NOT CHANGE THIS METHOD!
void randomizeTrials() {
    for (int i = 0; i < 16; i++) { // 4 rows times 4 columns = 16 target
        for (int k = 0; k < NUM_REPEATS; k++) { // each target will repeat 'NUM_REPEATS' times
            trials.add(i);
        }
    }
    Collections.shuffle(trials);             // randomize the trial order

    System.out.println("trial order: " + trials);    // prints trial order - for debug purposes
}

// Print results at the end of the study
void printResults(float timeTaken, float penalty) {
    float accuracy0 = (float)hits*100f/(float)(hits+misses);
    float accuracy1 = width / 2;
    float accuracy2 = height / 2 + 60;

    if (accuracy0 >= 95.0) {
        winSound.play();
    } else {
        loseSound.play();
    }

    background(0);       // clears screen
    fill(255);    //set text fill color to white
    text(day() + "/" + String.format("%02d",month()) + "/" + year() + "  " + String.format("%02d", hour()) + ":" + String.format("%02d", minute()) + ":" + String.format("%02d", second()) , 100, 20);   // display time on screen

    text("Finished!", width / 2, height / 2);
    text("Hits: " + hits, width / 2, height / 2 + 20);
    text("Misses: " + misses, width / 2, height / 2 + 40);
    text("Accuracy: " + accuracy0 + "%", accuracy1, accuracy2);
    text("Total time taken: " + timeTaken + " sec", width / 2, height / 2 + 80);
    text("Average time for each target: " + nf((timeTaken)/(float)(hits+misses),0,3) + " sec", width / 2, height / 2 + 100);
    text("Average time for each target + penalty: " + nf(((timeTaken)/(float)(hits+misses) + penalty),0,3) + " sec", width / 2, height / 2 + 140);

    saveFrame("results-######.png");    // saves screenshot in current folder
}

// Mouse button was released - lets test to see if hit was in the correct target
void mouseReleased() {
    if (trialNum >= trials.size()) { // if study is over, just return
        return;
    }
    if (trialNum == 0) { // check if first click, if so, start timer
        startTime = millis();
    }
    if (trialNum == trials.size() - 1) {         // check if final click
        finishTime = millis();    // save final timestamp
        println("We're done!");
    }

    Target target = getTargetBounds(trials.get(trialNum));    // get the location and size for the target in the current trial

    // Check to see if mouse cursor is inside the target bounds
    if(dist(target.x, target.y, mouseX, mouseY) < (target.w/2)) {
        hitSound.play();
        System.out.println("HIT! " + trialNum + " " + (millis() - startTime));     // success - hit!
        hits++; // increases hits counter
    } else {
        missSound.play();
        System.out.println("MISSED! " + trialNum + " " + (millis() - startTime));  // fail
        misses++;   // increases misses counter
    }

    trialNum++;   // move on to the next trial; UI will be updated on the next draw() cycle
}

// For a given target ID, returns its location and size
Target getTargetBounds(int i) {
    int x = (int)LEFT_PADDING +
                (int)((i % 4) * (TARGET_SIZE + TARGET_PADDING) + MARGIN);
    int y = (int)TOP_PADDING +
                (int)((i / 4) * (TARGET_SIZE + TARGET_PADDING) + MARGIN);

    return new Target(x, y, TARGET_SIZE);
}

// Draw target on-screen
// This method is called in every draw cycle; you can update the target's UI here
void drawTarget(int i) {
    Target target = getTargetBounds(i);   // get the location and size for the circle with ID:i

    // check whether current circle is the intended target
    if (trials.get(trialNum) == i) {
        fill(167, 255, 50);
        // fill(235, 92, 255);           // fill pinkish
    } else {
        fill(64, 64, 64);           // fill dark gray
    }

    circle(target.x, target.y, target.w);   // draw target

    noStroke();    // next targets won't have stroke (unless it is the intended target)
}
