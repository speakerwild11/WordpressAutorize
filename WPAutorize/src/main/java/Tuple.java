public class Tuple<X, Y>{
    public X x;
    public Y y;

    public Tuple(X x, Y y){
        this.x = x;
        this.y = y;
    }

    public void setX(X obj){
        this.x = obj;
    }

    public void setY(Y obj){
        this.y = obj;
    }
}
