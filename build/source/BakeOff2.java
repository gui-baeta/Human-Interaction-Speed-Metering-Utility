import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.lang.IndexOutOfBoundsException; 
import java.lang.NullPointerException; 
import java.util.Collections; 
import processing.sound.SoundFile; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class BakeOff2 extends PApplet {

// Bakeoff #2 - Seleção de Alvos e Fatores Humanos //<>// //<>// //<>//
// IPM 2019-20, Semestre 2
// Bake-off: durante a aula de lab da semana de 20 de Abril
// Submissão via Twitter: exclusivamente no dia 24 de Abril, até às 23h59

// Processing reference: https://processing.org/reference/






SoundFile hitSound;
SoundFile missSound;
SoundFile winSound;
SoundFile loseSound;

// Target properties
float PPI, PPCM;
float SCALE_FACTOR;
float TARGET_SIZE;
float TARGET_PADDING, MARGIN, LEFT_PADDING, TOP_PADDING;

// Study properties
ArrayList<Integer> trials  = new ArrayList<Integer>();    // contains the order of targets that activate in the test
int trialNum               = 0;                           // the current trial number (indexes into trials array above)
final int NUM_REPEATS      = 3;                           // sets the number of times each target repeats in the test - FOR THE BAKEOFF NEEDS TO BE 3!
boolean ended              = false;
int selected_target = 0;

// Performance variables
int startTime              = 0;      // time starts when the first click is captured
int finishTime             = 0;      // records the time of the final click
int hits                   = 0;      // number of successful clicks
int misses                 = 0;      // number of missed clicks

class CursorPrev {
    int x;
    int y;

    CursorPrev(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

ArrayList<Double> difficulty_indexes;
CursorPrev cursor_prev;

// Class used to store properties of a target
class Target {
    int x, y;
    float w;

    Target(int posx, int posy, float twidth) {
        x = posx;
        y = posy;
        w = twidth;
    }
}

public double customLog(double base, double number) {
    return Math.log(number) / Math.log(base);
}

// Setup window and vars - runs once
public void setup() {
    //size(900, 900);              // window size in px (use for debugging)
                    // USE THIS DURING THE BAKEOFF!

    SCALE_FACTOR    = 1.0f / displayDensity();            // scale factor for high-density displays
    String[] ppi_string = loadStrings("ppi.txt");        // The text from the file is loaded into an array.
    PPI            = PApplet.parseFloat(ppi_string[1]);               // set PPI, we assume the ppi value is in the second line of the .txt
    PPCM           = PPI / 2.54f * SCALE_FACTOR;          // do not change this!
    TARGET_SIZE    = 1.5f * PPCM;                         // set the target size in cm; do not change this!
    TARGET_PADDING = 1.5f * PPCM;                         // set the padding around the targets in cm; do not change this!
    MARGIN         = 1.5f * PPCM;                         // set the margin around the targets in cm; do not change this!
    LEFT_PADDING   = width/2 - TARGET_SIZE - 1.5f*TARGET_PADDING - 1.5f*MARGIN;        // set the margin of the grid of targets to the left of the canvas; do not change this!
    TOP_PADDING    = height/2 - TARGET_SIZE - 1.5f*TARGET_PADDING - 1.5f*MARGIN;       // set the margin of the grid of targets to the top of the canvas; do not change this!

    difficulty_indexes = new ArrayList<Double>();

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
    missSound.amp(0.01f);
    winSound.amp(0.1f);
    loseSound.amp(0.1f);

    // noCursor();
    cursor(CROSS);
}

// Updates UI - this method is constantly being called and drawing targets
public void draw() {

    if (hasEnded()) {
        return; // nothing else to do; study is over
    }

    background(0);       // set background to black

    // Print trial count
    fill(100);          // set text fill color to white
    text("Trial " + (trialNum + 1) + " of " + trials.size(), 70, 20);    // display what trial the participant is on (the top-left corner)

    selected_target = closestTarget(); // get the closest target to the mouse

    cursorBackdrop();

    // Draw targets
    for (int i = 0; i < 16; i++) {
        drawTarget(i);
    }
}

public void cursorBackdrop() {
    Target closest = getTargetBounds(selected_target);
    Target current_target = getTargetBounds(trials.get(trialNum));

    float radius = 2 * max(dist(mouseX, mouseY, closest.x, closest.y) - closest.w / 2, TARGET_SIZE / 2);

    stroke(55, 55, 55);
    strokeWeight(6);
    line(mouseX, mouseY, current_target.x, current_target.y);
    noStroke();

    fill(55, 55, 55);
    circle(mouseX, mouseY, radius);   // draw mouse backdrop
}

public boolean hasEnded() {
    if (ended) return true;    // returns if test has ended before

    // Check if the study is over
    if (trialNum >= trials.size()) {
        float timeTaken = (finishTime-startTime) / 1000f;     // convert to seconds - DO NOT CHANGE!
        float penalty = constrain(((95f - ((float)hits * 100f / (float)(hits + misses))) * .2f), 0, 100);    // calculate penalty - DO NOT CHANGE!



        printResults(timeTaken, penalty);    // prints study results on-screen
        ended = true;
    }

    return ended;
}

// Randomize the order in the targets to be selected
// DO NOT CHANGE THIS METHOD!
public void randomizeTrials() {
    for (int i = 0; i < 16; i++) {             // 4 rows times 4 columns = 16 target
        for (int k = 0; k < NUM_REPEATS; k++) {  // each target will repeat 'NUM_REPEATS' times
            trials.add(i);
        }
    }
    Collections.shuffle(trials);             // randomize the trial order

    System.out.println("trial order: " + trials);    // prints trial order - for debug purposes
}

// Print results at the end of the study
public void printResults(float timeTaken, float penalty) {
    float accuracy0 = (float)hits * 100f / (float)(hits + misses);

    if (accuracy0 >= 95.0f) winSound.play();
    else loseSound.play();

    background(0);       // clears screen
    fill(209);    //set text fill color to white
    text(day() + "/" + String.format("%02d",month()) + "/" + year() + "  " + String.format("%02d", hour()) + ":" + String.format("%02d", minute()) + ":" + String.format("%02d", second()) , 100, 20);   // display time on screen

    text("Finished!", width / 2, height / 2 - 400);
    text("Hits: " + hits, width / 2, height / 2 - 380);
    text("Misses: " + misses, width / 2, height / 2 - 360);
    text("Accuracy: " + accuracy0 + "%", width / 2, height / 2 - 340);
    text("Total time taken: " + timeTaken + " sec", width / 2, height / 2 - 320);
    text("Average time for each target: " + nf((timeTaken) / (float)(hits + misses), 0, 3) + " sec", width / 2, height / 2 - 300);
    text("Average time for each target + penalty: " + nf(((timeTaken) / (float)(hits + misses) + penalty), 0, 3) + " sec", width / 2, height / 2 - 260);

    rectMode(CORNERS);
    fill(15);
    rect(width / 2 - 370, height / 2 - 200, width / 2 + 370, height / 2 + 460, 20);
    fill(227);
    text("Fitts Performance Index", width / 2, height / 2 - 175);

    int target_id = 1;
    float x = width / 2 - 200, y = height / 2 - 140;

    text("Target " + target_id + ": N/A", x, y);
    y = y + 25;
    target_id++;

    for (double p_i : difficulty_indexes) {
        if (p_i == -1) {
            text("Target " + target_id + ": Missed!", x, y);
        } else {
            text("Target " + target_id + ": " + String.format("%.4f", p_i), x, y);
        }
        if (target_id == 24) {
            x = x + 400;
            y = height / 2 - 140;
        } else {
            y = y + 25;
        }
        target_id++;
    }

    saveFrame("results-######.png");    // saves screenshot in current folder
}

// Mouse button was released - lets test to see if hit was in the correct target
public void mouseReleased() {
    if (trialNum >= trials.size()) {
        return;      // if study is over, just return
    }

    if (trialNum == 0) {
        startTime = millis();    // check if first click, if so, start timer
    }

    if (trialNum == trials.size() - 1) {        // check if final click
        finishTime = millis();    // save final timestamp
        println("We're done!");
    }

    Target target = getTargetBounds(trials.get(trialNum));    // get the location and size for the target in the current trial

    double displacement = -1;

    // Check to see if mouse cursor is inside the target bounds
    if (trials.get(trialNum) == selected_target) { // HIT
        hitSound.play();
        hits++; // increases hits counter
        if (trialNum > 0) {
            displacement = dist(target.x, target.y, cursor_prev.x, cursor_prev.y);
            difficulty_indexes.add(customLog(2, (displacement / TARGET_SIZE) + 1));
            System.out.println("HIT! " + trialNum + " " + (millis() - startTime) + " Performance Index: " + String.format("%.4f", difficulty_indexes.get(trialNum - 1)));     // success - hit!
        } else {
            System.out.println("HIT! " + trialNum + " " + (millis() - startTime) + " Performance Index: N/A");     // success - hit!
        }
    } else {
        missSound.play();
        System.out.println("MISSED! " + trialNum + " " + (millis() - startTime));  // fail
        misses++;   // increases misses counter
        displacement = -1;
        if (trialNum > 0) {
            difficulty_indexes.add(displacement);
        }
    }

    cursor_prev = new CursorPrev(mouseX, mouseY);

    trialNum++;   // move on to the next trial; UI will be updated on the next draw() cycle
}

// For a given target ID, returns its location and size
public Target getTargetBounds(int i) {
    int x = (int)LEFT_PADDING + (int)((i % 4) * (TARGET_SIZE + TARGET_PADDING) + MARGIN);
    int y = (int)TOP_PADDING + (int)((i / 4) * (TARGET_SIZE + TARGET_PADDING) + MARGIN);

    return new Target(x, y, TARGET_SIZE);
}

// Draw target on-screen
// This method is called in every draw cycle; you can update the target's UI here
public void drawTarget(int i) {
    Target target = getTargetBounds(i);   // get the location and size for the circle with ID:i

    if(i == selected_target) {
        fill(55, 55, 55);
        circle(target.x, target.y, target.w * 1.3f);
    }

    // check whether current circle is the intended target
    if (trials.get(trialNum) == i) {  // fill yellow
        fill(255, 255, 0); // Yellow
    } else { // Target not to hit!
        fill(119, 119, 119);           // fill dark gray
    }

    circle(target.x, target.y, target.w);   // draw target

    try {
        if (trials.get(trialNum + 1) == i) {
            fill(0, 0);
            stroke(250, 250, 0, 100);
            strokeWeight(5);
            circle(target.x, target.y, target.w * 1.2f);

            if(trials.get(trialNum) == trials.get(trialNum + 1)) {
                fill(0, 0, 0);
                text("2x", target.x, target.y + 3);
            }
        }
    } catch (IndexOutOfBoundsException e) {}

    if(i == selected_target) {
        if(i == trials.get(trialNum)) stroke(0, 255, 0, 50);
        else stroke(255, 0, 0, 50);

        fill(255, 255, 255, 50);
        circle(target.x, target.y, target.w);
    }

    noStroke();
}

public int closestTarget(){
    int x;
    int y;
    float target_space = TARGET_SIZE + TARGET_PADDING;

    if(mouseX <= LEFT_PADDING) x = 0;
    else if(mouseX >= LEFT_PADDING + (target_space) * 3) x = 3;
    else x = floor((mouseX - LEFT_PADDING) / (target_space));

    if(mouseY <= TOP_PADDING) y = 0;
    else if(mouseY >= TOP_PADDING + (target_space) * 3) y = 3;
    else y = floor((mouseY - TOP_PADDING) / (target_space));

    return x + 4*y;
}
  public void settings() {  fullScreen(); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "BakeOff2" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
