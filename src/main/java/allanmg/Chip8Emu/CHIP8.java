package allanmg.Chip8Emu;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

enum State{
    PAUSED, STOPED, RUNNING, CRASHED
}
public class CHIP8 {
    State state ;
    Window window;
    public static void main(String[] args){
        new Window(10);
    }
    CPU cpu;
    MEM mem;

    public boolean[] keyStatus = new boolean[16];
    boolean keyEvent;
    public int keySet;

    public CHIP8(Window window){
        this.window = window;

        mem = new MEM(this);
        cpu = new CPU(this);

        state = State.STOPED;
        cpu.run();
    }

    public void setKey(int address, boolean key){
        keyEvent = true;
        keyStatus[address] = key;
        keySet = address;
    }

    public void render(){
        window.draw(g -> {
            for(int x = 0; x < 64; x++){
                for(int y = 0; y < 32; y++){
                    if(cpu.getpixel(x,y)) {
                        g.setColor(Color.green);
                    }else {
                        g.setColor(Color.DARK_GRAY);
                    }
                    g.fillRect(x,y,1,1);
                }
            }
        });
    }

    Path loadedROM;
    public void load(Path path){
        if(state != State.STOPED){
            stop();
        }
        loadedROM = path;
        mem.copyROM(path);
        start();
    }

    public void start(){
        if(state == State.CRASHED){
            cpu.run();
        }
        state = State.RUNNING;
    }
    public void pause(){
        state = State.PAUSED;
    }
    public void stop(){
        cpu.stop();
    }
    public void reset(){
        cpu.reset();
    }
    public void step(){
        cpu.fetch();
    }

}
class CPU{
    CHIP8 chip8;
    MEM mem;
    Instructions ins;

    int cpuHz = 300;

    boolean[][] display = new boolean[32][64];

    short[] stack;
    short pc;
    short i;

    int[] v;
    int sp;
    int dt;
    int st;

    CPU(CHIP8 chip8){
        this.chip8 = chip8;
        this.mem = chip8.mem;

        this.ins = new Instructions(chip8, this, mem);

        stack = new short[16];
        v = new int[16];

        pc = 0x200;
        sp = 0;
        i = 0;
    }
    public void reset(){
        stop();
        mem.copyROM(chip8.loadedROM);
        chip8.state = State.RUNNING;
    }
    void stop(){
        chip8.state = State.STOPED;
        mem.clear();
        stack = new short[16];
        v = new int[16];

        pc = 0x200;
        sp = 0;
        i = 0;

        dt = 0;
        st = 0;

        clrDisplay();

    }
    public void block(){
        //blocks cpu operations
        pc -= 2;
    }
    public void free(){
        //free cpu operations
        nextInstruction();
    }
    //loop
    void run(){
        System.out.println("starting loop");
        Thread CPU = new Thread(() -> {
            final long time = 1000/cpuHz;
            while (true) {
                long startTime = System.currentTimeMillis();
                chip8.render();
                if(chip8.state == State.RUNNING){
                    fetch();

                    if(st > 0){
                        System.out.println("beped");
                        st --;
                    }
                    if(dt > 0) {
                        dt--;
                    }

                }

                long elapsedTime = System.currentTimeMillis() - startTime;

                if (elapsedTime < time) {
                    try {
                        Thread.sleep(time - elapsedTime);
                    } catch (InterruptedException e) {
                        chip8.state = State.CRASHED;
                        System.out.println(" <!!> CHIP 8 EMU CRASHED");
                        e.printStackTrace();
                    }
                }
            }
        });
        CPU.start();
    }
    void fetch() {
        short opcode = (short)((mem.get(pc) << 8) & 0xff00| mem.get((short)(pc+1)) & 0x00ff);
        nextInstruction();
        ins.decode(opcode);
        chip8.keyEvent = false;
    }
    public void nextInstruction(){
        pc += 2;
    }
    public void jumpTo(short addr){
        pc = addr;
    }

    //graphics
    public void setPixel(int x, int y){
        if(x >= 64){
            x -= 64;
        }else if(x < 0){
            x += 64;
        }
        if(y >= 64){
            y -= 64;
        }else if(y < 0){
            y += 64;
        }
        display[y][x] ^= true;
    }
    public boolean getpixel(int x, int y){
        if(x >= 64){
            x -= 64;
        }else if(x < 0){
            x += 64;
        }
        if(y >= 64){
            y -= 64;
        }else if(y < 0){
            y += 64;
        }
        return display[y][x];
    }
    void clrDisplay(){
        display = new boolean[32][64];

    }
}
class MEM {
    CHIP8 chip8;
    int[] sprites = {
            0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
            0x20, 0x60, 0x20, 0x20, 0x70, // 1
            0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
            0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
            0x90, 0x90, 0xF0, 0x10, 0x10, // 4
            0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
            0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
            0xF0, 0x10, 0x20, 0x40, 0x40, // 7
            0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
            0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
            0xF0, 0x90, 0xF0, 0x90, 0x90, // A
            0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
            0xF0, 0x80, 0x80, 0x80, 0xF0, // C
            0xE0, 0x90, 0x90, 0x90, 0xE0, // D
            0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
            0xF0, 0x80, 0xF0, 0x80, 0x80  // F
    };

    int[] memory;

    MEM(CHIP8 chip8) {
        this.chip8 = chip8;
        memory = new int[4096];
        loadSprites();

    }

    public void loadSprites() {
        for (short i = 0; i < sprites.length; i++) {
            write((char) sprites[i], i);
        }
    }

    public void copyROM(Path path) {
        try {
            byte[] romData;
            romData = Files.readAllBytes(path);
            for (short i = 0; i < romData.length; i++) {
                write(romData[i], (0x200 + i));
            }

            System.out.println("============ROM LOADED============\n" +
                    " >> Rom Name: " + path.getFileName() + "\n" +
                    "  > " + romData.length + " Bytes\n" +
                    "==================================");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void write(int value, int adress) {
        memory[adress & 0xfff] = value & 0xff;
    }

    public int get(int addr) {
        return memory[addr & 0xfff];
    }

    public void clear() {
        for (short i = 0x200; i < memory.length; i++) {
            memory[i] = 0x00;
        }
    }

    public void displayMemory() {
        for (short i = 0; i < memory.length; i++) {
            if (i % 8 == 0) {
                System.out.print("\n");
            }
            System.out.print(String.format("0x%03X", i & 0xfff) + "[" + String.format("0x%02X", memory[i] & 0xff) + "] | ");
        }
    }
}
class Instructions{
    CPU cpu;
    MEM mem;
    CHIP8 chip8;
    public Instructions(CHIP8 chip8, CPU cpu, MEM mem){
        this.chip8 = chip8;
        this.cpu = cpu;
        this.mem = mem;
    }
    public void decode(short opcode){
        switch (opcode & 0xF000) {
            case 0x0000:
                switch (opcode) {
                    case 0x00E0:
                        x00E0();
                        break;
                    case 0x00EE:
                        x00EE();
                        break;
                }
                break;
            case 0x1000:
                x1000(opcode);
                break;
            case 0x2000:
                x2000(opcode);
                break;
            case 0x3000:
                x3000(opcode);
                break;
            case 0x4000:
                x4000(opcode);
                break;
            case 0x5000:
                x5000(opcode);
                break;
            case 0x6000:
                x6000(opcode);
                break;
            case 0x7000:
                x7000(opcode);
                break;
            case 0x8000:
                switch (opcode & 0xF) {
                    case 0x0:
                        x8000(opcode);
                        break;
                    case 0x1:
                        x8001(opcode);
                        break;
                    case 0x2:
                        x8002(opcode);
                        break;
                    case 0x3:
                        x8003(opcode);
                        break;
                    case 0x4:
                        x8004(opcode);
                        break;
                    case 0x5:
                        x8005(opcode);
                        break;
                    case 0x6:
                        x8006(opcode);
                        break;
                    case 0x7:
                        x8007(opcode);
                        break;
                    case 0xE:
                        x800E(opcode);
                        break;
                }

                break;
            case 0x9000:
                x9000(opcode);
                break;
            case 0xA000:
                xA000(opcode);
                break;
            case 0xB000:
                xB000(opcode);
                break;
            case 0xC000:
                xC000(opcode);
                break;
            case 0xD000:
                xD000(opcode);
                break;
            case 0xE000:
                switch (opcode & 0xFF) {
                    case 0x9E:
                        xE09E(opcode);
                        break;
                    case 0xA1:
                        xE0A1(opcode);
                        break;
                }

                break;
            case 0xF000:
                switch (opcode & 0xFF) {
                    case 0x07:
                        xF007(opcode);
                        break;
                    case 0x0A:
                        xF00A(opcode);
                        break;
                    case 0x15:
                        xF015(opcode);
                        break;
                    case 0x18:
                        xF018(opcode);
                        break;
                    case 0x1E:
                        xF01E(opcode);
                        break;
                    case 0x29:
                        xF029(opcode);
                        break;
                    case 0x33:
                        xF033(opcode);
                        break;
                    case 0x55:
                        xF055(opcode);
                        break;
                    case 0x65:
                        xF065(opcode);
                        break;
                }

                break;
            default:
                System.out.println("uknow opcode " + String.format("0x%04X", opcode & 0xffff));
        }
    }
    void x00E0(){
        cpu.clrDisplay();
    }
    void x00EE(){
        cpu.pc = cpu.stack[cpu.sp];
        cpu.sp --;
    }
    void x1000(short opcode){
        short nnn = (short)(opcode & 0x0fff);
        cpu.jumpTo(nnn);
    }
    void x2000(short opcode){
        short nnn = (short)(opcode & 0x0fff);
        cpu.sp++;
        cpu.stack[cpu.sp] = cpu.pc;
        cpu.jumpTo(nnn);
    }
    void x3000(short opcode){
        int x = ((opcode & 0x0f00) >> 8);
        int nn = (opcode & 0x00ff);
        if (cpu.v[x] == nn) {
            cpu.nextInstruction();
        }
    }
    void x4000(short opcode){
        int x =  ((opcode & 0x0f00) >> 8);
        int nn = (opcode & 0x00ff);
        if (cpu.v[x] != nn) {
            cpu.nextInstruction();
        }
    }
    void x5000(short opcode){
        int x = ((opcode & 0x0f00) >> 8);
        int y = ((opcode & 0x00f0) >> 4);
        if(cpu.v[x] == cpu.v[y]){
            cpu.nextInstruction();
        }
    }
    void x6000(short opcode){
        int x = ((opcode & 0x0f00) >> 8);
        int nn = (opcode & 0x00ff);
        cpu.v[x] = nn;
    }
    void x7000(short opcode){
        int x = ((opcode & 0x0f00) >> 8);
        int nn = (opcode & 0x00ff);
        cpu.v[x] = (cpu.v[x] + nn)&0xff;
    }
    void x8000(short opcode){
        int x = ((opcode & 0x0f00) >> 8);
        int y = ((opcode & 0x00f0) >> 4);
        cpu.v[x] = cpu.v[y];
    }
    void x8001(short opcode){
        cpu.v[0xf] = 0;
        int x = ((opcode & 0x0f00) >> 8);
        int y = ((opcode & 0x00f0) >> 4);
        cpu.v[x] |= cpu.v[y];
    }
    void x8002(short opcode){
        cpu.v[0xf] = 0;
        int x = ((opcode & 0x0f00) >> 8);
        int y = ((opcode & 0x00f0) >> 4);
        cpu.v[x] &= cpu.v[y];
    }
    void x8003(short opcode){
        cpu.v[0xf] = 0;
        int x = ((opcode & 0x0f00) >> 8);
        int y = ((opcode & 0x00f0) >> 4);
        cpu.v[x] ^= cpu.v[y];
    }
    void x8004(short opcode){
        int x = ((opcode & 0x0f00) >> 8);
        int y = ((opcode & 0x00f0) >> 4);
        int sum =  (cpu.v[x]+cpu.v[y]);
        cpu.v[0xf] = 0;
        if(sum > 0xff){
            cpu.v[0xf] = 1;
        }
        cpu.v[x] = (sum & 0xff);

    }
    void x8005(short opcode){
        int x = ((opcode & 0x0f00) >> 8);
        int y = ((opcode & 0x00f0) >> 4);
        int sub = (cpu.v[x]-cpu.v[y]);
        cpu.v[0xf] = 0;
        if(sub < 0x0){
            cpu.v[0xf] = 1;
        }
        cpu.v[x] = (sub & 0xff);
    }
    void x8006(short opcode){
        int x = ((opcode & 0x0f00) >> 8);

        cpu.v[0xf] = (cpu.v[x] & 0x01);
        cpu.v[x] = (cpu.v[x] >> 1) & 0xff;
    }

    void x8007(short opcode){
        int x = ((opcode & 0x0f00) >> 8);
        int y = ((opcode & 0x00f0) >> 4);
        cpu.v[0xf] = 0;
        if(cpu.v[x] < cpu.v[y]){
            cpu.v[0xf] = 1;
        }
        cpu.v[x] = (cpu.v[y] - cpu.v[x]);
    }
    void x800E(short opcode){
        int x = ((opcode & 0x0f00) >> 8);

        cpu.v[0xf] = (cpu.v[x] & 0x80);
        cpu.v[x] = (cpu.v[x] << 1) & 0xff;
    }
    void x9000(short opcode){
        int x = ((opcode & 0x0f00) >> 8);
        int y = ((opcode & 0x00f0) >> 4);
        if(cpu.v[x] != cpu.v[y]){
            cpu.nextInstruction();
        }
    }
    void xA000(short opcode){
        short nnn = (short)(opcode & 0xfff);
        cpu.i = nnn;
    }
    void xB000(short opcode){
        short nnn = (short)(opcode & 0xfff);
        cpu.jumpTo((short) (nnn + cpu.v[0]));
    }
    void xC000(short opcode){
        Random random = new Random();
        int x = ((opcode & 0x0f00) >> 8);
        int nn = (opcode & 0x00ff);
        cpu.v[x] = (random.nextInt(255) & nn);
    }
    void xD000(short opcode){
        int x = cpu.v[(opcode & 0x0F00) >> 8];
        int y = cpu.v[(opcode & 0x00F0) >> 4];
        int height = opcode & 0x000F;
        cpu.v[0xF] = 0;
        for (int yline = 0; yline < height; yline++)
        {
            int pixel = mem.get((short) (cpu.i + yline));
            for (int xline = 0; xline < 8; xline++)
            {
                if ((pixel & (0x80 >> xline)) != 0)
                {
                    if (cpu.getpixel((x + xline)%64, (y + yline)%32)) {
                        cpu.v[0xF] = 1;
                    }
                    cpu.setPixel((x + xline)%64, (y + yline)%32);
                }
            }
        }
    }
    void xE09E(short opcode){
        int x = ((opcode & 0x0f00) >> 8);
        if(cpu.chip8.keyStatus[cpu.v[x]]){
            cpu.nextInstruction();
        }
    }
    void xE0A1(short opcode){
        int x = ((opcode & 0x0f00) >> 8);
        if(!cpu.chip8.keyStatus[cpu.v[x]]){
            cpu.nextInstruction();
        }
    }
    void xF007(short opcode){
        int x = ((opcode & 0x0f00) >> 8);
        cpu.v[x] = cpu.dt;
    }
    void xF00A(short opcode){
        int x = ((opcode & 0x0f00) >> 8);
        cpu.block();
        if(chip8.keyEvent) {
            System.out.println("event");
            cpu.v[x] = chip8.keySet & 0xf;
            cpu.free();
        }
    }
    void xF015(short opcode){
        int x =  ((opcode & 0x0f00) >> 8);
        cpu.dt = cpu.v[x];
    }
    void xF018(short opcode){
        int x = ((opcode & 0x0f00) >> 8);
        cpu.st = cpu.v[x];
    }
    void xF01E(short opcode){
        int x = ((opcode & 0x0f00) >> 8);
        cpu.i += cpu.v[x];
    }
    void xF029(short opcode){
        int x = ((opcode & 0x0f00) >> 8);
        cpu.i += cpu.v[x]*5;
    }
    void xF033(short opcode){
        int x = ((opcode & 0x0f00) >> 8);
        mem.write((cpu.v[x] / 100), cpu.i);
        mem.write(((cpu.v[x] / 10) % 10), (cpu.i + 1));
        mem.write(((cpu.v[x] % 100) % 10), (cpu.i + 2));

    }
    void xF055(short opcode){
        int x = ((opcode & 0x0f00) >> 8);
        for (int i = 0; i <= x; i++) {
            mem.write(cpu.v[i], (cpu.i + i));
        }
        cpu.i += x+1;
    }
    void xF065(short opcode){
        int x = ((opcode & 0x0f00) >> 8);
        for (int i = 0; i <= x; i++) {
            cpu.v[i] = mem.get(cpu.i + i);
        }
        cpu.i += x+1;
    }

}
