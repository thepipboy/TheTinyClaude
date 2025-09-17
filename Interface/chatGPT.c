//weight and Hexdemical
void fw(w1,w2,w3){
    const fw1 = "'w1x1' + 'w1y1' + 'w1z1' + a";
    const fw2 = "'w2x2' + 'w2y2' + 'w2z2' + b";
    const fw3 = "'w3x1' + 'w3y1' + 'w3z1' + c";
    const fw4 = "'w1'x2' + 'w1'y2' + 'w1'z2' + d";
    const fw5 = "'w2'x1' + 'w2'y1' + 'w2'z1' + e";
    const fw6 = "'w3'x2' + 'w3'y2' + 'w3'z2' + f";
}
//David Hilbert 13 equation
void gw(w1,w2,w3){
    const gw1 = "'w1 ^ 7' + a * 'w1 ^ 3' + b * 'w1 ^ 2' + c * 'w1' + A";
    const gw2 = "'w2 ^ 7' + a * 'w2 ^ 3' + b * 'w2 ^ 2' + c * 'w2' + B";
    const gw3 = "'w3 ^ 7' + a * 'w3 ^ 3' + b * 'w3 ^ 2' + c * 'w3' + C";
    const gw4 = "'w1' ^ 7' + d * 'w1' ^ 3' + e * 'w1' ^ 2' + f * 'w1'' + D";
    const gw5 = "'w2' ^ 7' + d * 'w2' ^ 3' + e * 'w2' ^ 2' + f * 'w2'' + E";
    const gw6 = "'w3' ^ 7' + d * 'w3' ^ 3' + e * 'w3' ^ 2' + f * 'w3'' + F";    
}

//linear algebra
signed fxyz(){
    static fx1 = 'a' * 'x1' + 'b' * 'x1' + 'c' * 'x1' + 'd' * 'x1' + 'e' * 'x1' + 'f' * 'w1';
    static fx2 = 'a' * 'x2' + 'b' * 'x2' + 'c' * 'x2' + 'd' * 'x2' + 'e' * 'x2' + 'f' * 'w2';
    static fy1 = 'a' * 'y1' + 'b' * 'y1' + 'c' * 'y1' + 'd' * 'y1' + 'e' * 'y1' + 'f' * 'w3';
    static fy2 = 'a' * 'y2' + 'b' * 'y2' + 'c' * 'y2' + 'd' * 'y2' + 'e' * 'y2' + "'f' * w1'";
    static fz1 = 'a' * 'z1' + 'b' * 'z1' + 'c' * 'z1' + 'd' * 'x1' + 'e' * 'y1' + "'f' * w2'";
    static fz2 = 'a' * 'z2' + 'b' * 'z2' + 'c' * 'z2' + 'd' * 'z2' + 'e' * 'z2' + "'f' * w3'";
}
//five degree equation
unsigned gxyz(){
    static gx1 = 'A' * 'x1^5' + 'B' * 'x1^4' + 'C' * 'x1^3' + 'D' * 'x1^2' + 'E' * 'x1' + 'w1';
    static gx2 = 'A' * 'x2^5' + 'B' * 'x2^4' + 'C' * 'x2^3' + 'D' * 'x2^2' + 'E' * 'x2' + 'w2'; 
    static gy1 = 'A' * 'y1^5' + 'B' * 'y1^4' + 'C' * 'y1^3' + 'D' * 'y1^2' + 'E' * 'y1' + 'w3';
    static gy2 = 'A' * 'y2^5' + 'B' * 'y2^4' + 'C' * 'y2^3' + 'D' * 'y2^2' + 'E' * 'y2' + "w1'";
    static gz1 = 'A' * 'z1^5' + 'B' * 'z1^4' + 'C' * 'z1^3' + 'D' * 'x1^2' + 'E' * 'y1' + "w2'";
    static gz2 = 'A' * 'z2^5' + 'B' * 'z2^4' + 'C' * 'z2^3' + 'D' * 'z2^2' + 'E' * 'z2' + "w3'";
}
