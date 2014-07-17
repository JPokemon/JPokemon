Emissary.defineController('org.jpokemon.overworld.PokemonTrainer', {
  view: false,
  width: 48,
  height: 56,
  tilesize: 32,
  lastMoveTime: null,
  moveSpeed: 320,

  constructor: function(playerJson) {
    this.name = playerJson.name;
    this.font = new me.Font('courier', 12, 'gray');
    this.x = this.lastx = playerJson.x;
    this.y = this.lasty = playerJson.y;
    this.moveQueue = [];

    this.renderable = new me.AnimationSheet(0, 0, me.loader.getImage(playerJson.avatar), this.width, this.height, 0);


    this.renderable.addAnimation('walkdown', [0, 1, 2, 1], 200);
    this.renderable.addAnimation('walkleft', [3, 4, 5, 4], 200);
    this.renderable.addAnimation('walkright', [6, 7, 8, 7], 200);
    this.renderable.addAnimation('walkup', [9, 10, 11, 10], 200);

    this.renderable.animationpause = true;
    this.renderable.setCurrentAnimation('walkdown');

    this.renderable.setAnimationFrame(1);
  },

  update: function() {
    if (this.moveQueue.length > 0) {
      debugger;
      var timeNow = new Date().getTime(),
          percentageMoved = (timeNow - this.lastMoveTime) / this.moveSpeed;

      if (percentageMoved > 1) {
        this.x = this.lastx = this.moveQueue[0].x;
        this.y = this.lasty = this.moveQueue[0].y;
        this.renderable.animationpause = true;
        this.moveQueue.shift();
      }
      else {
        if (this.x === this.lastx && this.y === this.lasty) {
          this.renderable.setCurrentAnimation(this.moveQueue[0].animation);
          this.renderable.animationpause = false;
        }

        // The subtraction accounts for the direction
        this.x = this.lastx + ((this.moveQueue[0].x - this.lastx) * percentageMoved);
        this.y = this.lasty + ((this.moveQueue[0].y - this.lasty) * percentageMoved);
      }
    }

    this.renderable.update();
  },

  getScreenX: function() {
    return this.x * this.tilesize - ((this.width - this.tilesize) / 2); // center the dude in the tile
  },

  getScreenY: function() {
    return this.y * this.tilesize - (this.height - this.tilesize); // dude at bottom of tile
  },

  draw: function(context) {
    var nameWidth = this.font.measureText(context, this.name).width, // + namePadding * 2;
        nameLeft = this.x * this.tilesize + (this.tilesize / 2) - (nameWidth / 2),
        nameTop = this.y * this.tilesize;

    context.save();

    context.globalAlpha = 0.75;
    context.fillStyle = 'black';
    context.fillRect(nameLeft, nameTop, nameWidth, 14);
    context.globalAlpha = 1.0;
    this.font.draw(context, this.name, nameLeft, nameTop);

    context.translate(this.getScreenX(), this.getScreenY());
    this.renderable.draw(context);

    context.restore();
  },

  addMoveToQueue: function(moveConfig) {
    this.moveQueue.push(moveConfig);
  }
});
