import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import ddf.minim.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class shooting_gallery_03 extends PApplet {

/*
  Shooting Gallery Game
  (cc) 2015-10-17 BY Pete Laric
  http://www.PeteLaric.com
  
  A simple shooting gallery game you can play with the mouse, or
  with the USB light gun controller I'm developing.  Written in
  Java using the Processing IDE & libraries.  Demo video:
  
    PSY 290: Shooting Gallery With Distractions
    https://www.youtube.com/watch?v=9KlTXRjKHoE
  
  All gun sounds are original and were recorded from a 3D printed
  Ruger Charger semi-automatic pistol in 2014 (a significant
  technical achievement at the time).  The gun range scene is from
  a local target range in rural New Mexico where the printed pistol
  was first tested.
  
  For more cool free and open source projects, including free
  music, check out my website: http://www.PeteLaric.com
*/

// from Bouncing Ball with Vectors by Daniel Shiffman.  
PVector location;  // Location of shape
PVector velocity;  // Velocity of shape
PVector gravity;   // Gravity acts at the shape's acceleration

// from PlayAFile

Minim minim;
AudioPlayer sound_player;

// from BackgroundImage
PImage bg1, bg2;

PImage bullet_image;

float target_size = 48;
float target_spin_angle = 0;
int cycle_count = 0;

int firing_now = 0;
int full_mag = 10; // number of rounds to load in a full mag
int distraction_mode = 0; // causes distractions in background

class Player { 
  int shots_fired;
  int hits;
  int misses;
  float hit_ratio;
  int score;
  int rounds_in_mag;
}

Player player_1 = new Player();
//Player player_2 = new Player();


public void setup()
{
  
  
  location = new PVector(100,0); //100,100
  velocity = new PVector(1.5f,2.1f);
  gravity = new PVector(0,0.2f);

  // we pass this to Minim so that it can load files from the data directory
  minim = new Minim(this);
  // loadFile will look in all the same places as loadImage does.
  // this means you can find files that are in the data folder and the 
  // sketch folder. you can also pass an absolute path, or a URL.
  sound_player = minim.loadFile("cock.wav");
  // play the file from start to finish.
  // if you want to play the file again, 
  // you need to call rewind() first.
  sound_player.play();
  
  player_1.rounds_in_mag = full_mag;
  
  noCursor();
  
  // The background image must be the same size as the parameters
  // into the size() method. In this program, the size of the image
  // is 640 x 360 pixels.
  
  load_background_image();
  
  bullet_image = loadImage("bullet_30x10.png");
}


public void load_background_image()
{
  if (distraction_mode != 0)
  {
    bg1 = loadImage("background_with_distractions_1.png");
    bg2 = loadImage("background_with_distractions_2.png");
  }
  else
  {
    bg1 = loadImage("background_no_LL.png");
    bg2 = loadImage("background_no_LL.png");
  }
}


public void show_magazine()
{
  int i = 0;
  int bullet_spacing = 10;
  for (i = 0; i < player_1.rounds_in_mag; i++)
  {
    image(bullet_image, 10, i * bullet_spacing + bullet_spacing);
  }
}


public void cross_hairs(float x, float y)
{
  stroke(color(0, 0, 0));
  noFill();
  float crosshair_size = 50;
  line(x - crosshair_size/2, y, x + crosshair_size/2, y);
  line(x, y - crosshair_size/2, x, y + crosshair_size/2);
  ellipse(x,y,crosshair_size,crosshair_size);
}


public void game_cycle()
{
  // all the typical stuff that happens when you aren't firing

  if (cycle_count < 25)
    background(bg1);
  else
    background(bg2);
  
  show_magazine();
  
  // Add velocity to the location.
  location.add(velocity);
  // Add gravity to velocity
  velocity.add(gravity);
  
  // Bounce off edges
  if ((location.x > width) || (location.x < 0))
  {
    velocity.x = velocity.x * -1;
  }
  if (location.y > height) // || (location.y < 0))
  {
    // We're reducing velocity ever so slightly 
    // when it hits the bottom of the window
    velocity.y = velocity.y * -0.95f; 
    location.y = height;
  }

  // Display circle at location vector
  //stroke(255); //255
  //strokeWeight(2);
  noStroke();
  fill(color(255, 0, 0)); //127
  float target_width = target_size * cos(target_spin_angle);
  ellipse(location.x,location.y,target_width,target_size);
  // white circle
  fill(color(255, 255, 255)); //127
  ellipse(location.x,location.y,target_width*2/3,target_size*2/3);
  // bullseye
  fill(color(255, 0, 0)); //127
  ellipse(location.x,location.y,target_width*1/3,target_size*1/3);
  if (target_spin_angle != 0)
  {
    target_spin_angle++;
    if (target_spin_angle >= 18)
    {
      target_spin_angle = 0;
      if (random(100) < 50) location.x = 0;
      else location.x = width;
      //location.x = random(width);
      location.y = random(height);
      velocity.x = random(10)-5;
      velocity.y = random(-3);
    }
  }
  
  // cross hairs
  cross_hairs(mouseX, mouseY);
  cross_hairs(mouseX-1, mouseY);
  cross_hairs(mouseX+1, mouseY);
  cross_hairs(mouseX, mouseY-1);
  cross_hairs(mouseX, mouseY+1);
  
  cycle_count++;
  if (cycle_count >= 50) cycle_count = 0;
}

public void draw()
{
  if (firing_now != 0)
  {
    // display white targets against a black background
    // (for light gun hit detection)
    //background(0);
    firing_now--; // switch back off; we just needed 1 cycle
    
    fire();
    game_cycle();
  }
  else
  {  
    game_cycle();
  }
  
}


public float compute_distance(float x1, float y1, float x2, float y2)
{
  float xdist = x2 - x1;
  float xdist_squared = pow(xdist, 2);
  float ydist = y2 - y1;
  float ydist_squared = pow(ydist, 2);
  float distance = sqrt(xdist_squared + ydist_squared);
  return distance;
}


public void reload()
{
  sound_player = minim.loadFile("cock.wav");
  sound_player.play();
  player_1.rounds_in_mag = full_mag;
  print("RELOADING!  rounds_in_mag: " + player_1.rounds_in_mag);
  println();
}


public void fire()
{
  if (player_1.rounds_in_mag > 0) // gotta have bullets to shoot
  {
    player_1.shots_fired++;
    player_1.rounds_in_mag--;
  
    float distance = compute_distance(location.x, location.y, mouseX, mouseY);
    if (distance <= target_size / 2)
    {
      // hit!
      player_1.hits++;
      sound_player = minim.loadFile("hit.wav");
      sound_player.play();
      target_spin_angle++;
      print("HIT!!!");
    }
    else
    {
      // miss
      player_1.misses++;
      sound_player = minim.loadFile("ricochet.wav");
      sound_player.play();
      print("MISS!");
    }
    
    print("  shots_fired: " + player_1.shots_fired);
    print("  hits: " + player_1.hits);
    print("  misses: " + player_1.misses);
    player_1.hit_ratio = 100 * (float)player_1.hits / (float)player_1.shots_fired;
    print("  hit_ratio: " + player_1.hit_ratio + "%");
    print("  rounds_in_mag: " + player_1.rounds_in_mag);
    println();
    
    delay(25);
  }
  else
  {
    // reload!
    reload();
  }
  
}


public void mousePressed()
{
  firing_now = 1;
  
  //fire();
}

public void mouseReleased()
{
  firing_now = 0;
}

public void keyPressed()
{
  if (key == 'd')
    distraction_mode = 1 - distraction_mode;
  load_background_image();
}
  public void settings() {  size(640,360);  smooth(); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "shooting_gallery_03" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
